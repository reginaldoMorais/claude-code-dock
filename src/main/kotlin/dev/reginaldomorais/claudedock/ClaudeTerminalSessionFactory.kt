package dev.reginaldomorais.claudedock

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.JBTerminalWidgetListener
import com.intellij.terminal.ui.TerminalWidget
import dev.reginaldomorais.claudedock.settings.ClaudeDockProjectSettings
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellStartupOptions
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.io.File

/**
 * Único ponto de acoplamento com a API de terminal da plataforma (RNF-15).
 *
 * `startShellTerminalWidget` é o mesmo caminho usado internamente por
 * `TerminalToolWindowManager.createShellWidget`, então os `LocalTerminalCustomizer`
 * registrados — inclusive o do plugin oficial da Anthropic, que injeta
 * `CLAUDE_CODE_SSE_PORT` — também se aplicam às sessões criadas aqui.
 *
 * Se um upgrade do IDE quebrar a integração, é este arquivo que precisa de revisão.
 */
object ClaudeTerminalSessionFactory {

    private val LOG = Logger.getInstance(ClaudeTerminalSessionFactory::class.java)

    /**
     * O que uma sessão pede à camada de cima.
     *
     * Existe para manter `JBTerminalWidgetListener` — API de terminal — dentro deste arquivo
     * (RNF-15). Quem implementa é [ClaudeDockSessions], que sabe de abas e panes.
     */
    interface SessionHost {
        fun openNewSession()
        fun splitSession(widget: TerminalWidget, stacked: Boolean)
        fun closeSession(widget: TerminalWidget)
        fun selectSiblingTab(next: Boolean)
        fun sessionFocused(widget: TerminalWidget)
    }

    /**
     * Cria uma sessão de terminal na raiz do projeto e executa [command] nela.
     *
     * O widget é filho de [parent], de modo que fechar a aba encerra o processo (RNF-09).
     */
    fun createSession(
        project: Project,
        parent: Disposable,
        command: String,
        host: SessionHost,
    ): TerminalWidget {
        val runner = LocalTerminalDirectRunner.createTerminalRunner(project)
        val configDir = ClaudeDockProjectSettings.getInstance(project).effectiveConfigDir()
        val options = ShellStartupOptions.Builder()
            .workingDirectory(ClaudeWorkingDirectory.resolve(project.basePath))
            .envVariables(ClaudeEnvironment.build(configDir))
            .build()

        // deferSessionStartUntilUiShown = true: o processo nasce quando a UI aparece (RNF-02).
        // A capa de carregamento fica *sobreposta*, e não no lugar do terminal, justamente para
        // que ele conte como visível aqui e receba o tamanho real da aba desde o primeiro frame.
        val widget = runner.startShellTerminalWidget(parent, options, true)
        // Fora da tool window "Terminal" a plataforma engole o Esc (RF-17).
        ClaudeEscapeForwarder.install(widget)
        // Cópia por seleção com o mouse (RF-26); play de TTS (RF-30); some junto com a aba.
        ClaudeSelectionCopyButton.install(widget, parent, project)
        // Respiro entre o conteúdo e as bordas da janela; o JediTerm só reserva 4px à esquerda.
        ClaudeSessionPadding.apply(
            widget.component,
            JBTerminalWidget.asJediTermWidget(widget)?.terminalPanel,
            ClaudeDockSettings.getInstance().effectivePadding(),
        )
        installHost(widget, host)
        widget.sendCommandToExecute(command)
        return widget
    }

    /**
     * Liga a sessão às ações do próprio widget e ao rastreio de foco (RF-36, RF-38).
     *
     * O menu de contexto do JediTerm monta "Split Right"/"Split Down" a partir deste listener:
     * `ShellTerminalWidget.getActions()` cria as ações com `TerminalSplitAction.create(…, listener)`,
     * cujo `isEnabled` chama `canSplit` e cujo `actionPerformed` chama `split`. Ou seja, o gatilho
     * nativo do split **já existia** — faltava alguém para atender (D-33).
     *
     * O ouvinte de foco mantém atualizado qual sessão é a "selecionada" da aba: com o split, uma
     * aba tem várias, e as ações do cabeçalho precisam saber sobre qual agir (D-34).
     */
    private fun installHost(widget: TerminalWidget, host: SessionHost) {
        val jediTermWidget = JBTerminalWidget.asJediTermWidget(widget) ?: return

        jediTermWidget.listener = object : JBTerminalWidgetListener {
            override fun onNewSession() = host.openNewSession()
            override fun onTerminalStarted() = Unit
            override fun onPreviousTabSelected() = host.selectSiblingTab(next = false)
            override fun onNextTabSelected() = host.selectSiblingTab(next = true)
            override fun onSessionClosed() = host.closeSession(widget)
            override fun showTabs() = Unit

            override fun canSplit(vertically: Boolean): Boolean = true

            /**
             * `vertically = true` é o "Split Right" do IDE — divisor vertical, panes lado a
             * lado. Confirmado nas presentations de `TerminalSplitAction$Companion`, que pareiam
             * `vertically` com o atalho `TW.SplitRight`.
             */
            override fun split(vertically: Boolean) =
                host.splitSession(widget, stacked = !vertically)
        }

        jediTermWidget.terminalPanel.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) = host.sessionFocused(widget)
        })
    }

    /**
     * Escreve [input] direto no PTY, como se o usuário tivesse digitado (RF-22).
     *
     * É o mesmo caminho do [ClaudeEscapeForwarder]. `sendCommandToExecute` não serve aqui:
     * ele lança `IOException` quando já há texto digitado no prompt, o que é a regra para
     * um TUI vivo como o do Claude Code.
     *
     * @return `false` quando ainda não há PTY ou a escrita falhou.
     */
    fun sendInput(widget: TerminalWidget, input: String): Boolean {
        val connector = widget.ttyConnector ?: return false

        return try {
            connector.write(input)
            true
        } catch (e: Exception) {
            LOG.warn("Falha ao escrever na sessão do Claude Code", e)
            false
        }
    }

    /**
     * Verifica se o executável configurado pode ser encontrado (CB-01, CB-02).
     *
     * É **advisório**: quem resolve o nome de verdade é o shell interativo da sessão, com o
     * `PATH` do `.zshrc`/`.bashrc`. Esta verificação enxerga menos que ele, e por isso não
     * bloqueia nada — no máximo notifica.
     *
     * Faz acesso a disco — não deve ser chamado na EDT (RNF-03).
     */
    fun isExecutableAvailable(
        executable: String,
        fallbackDirs: List<String> = DEFAULT_FALLBACK_DIRS,
    ): Boolean {
        val trimmed = executable.trim()
        if (trimmed.isEmpty()) return false

        // Caminho explícito: precisa existir e ser executável.
        if (trimmed.contains(File.separatorChar) || trimmed.contains('/')) {
            return File(trimmed).let { it.isFile && it.canExecute() }
        }

        if (PathEnvironmentVariableUtil.findInPath(trimmed) != null) return true

        return fallbackDirs.any { dir -> File(dir, trimmed).let { it.isFile && it.canExecute() } }
    }

    /**
     * Diretórios consultados quando o `PATH` do IDE não resolve o nome.
     *
     * `~/.local/bin` é onde o instalador oficial do Claude Code coloca o binário, e ele entra no
     * `PATH` por um arquivo de shell que o IDE nunca lê — o processo do IDE herda o ambiente da
     * sessão gráfica. Sem isto, uma instalação padrão gera notificação de "não encontrado" com o
     * CLI funcionando perfeitamente na sessão.
     *
     * ponytail: dois diretórios, não uma varredura.
     */
    private val DEFAULT_FALLBACK_DIRS: List<String> = listOf(
        File(System.getProperty("user.home"), ".local/bin").path,
        "/usr/local/bin",
    )
}
