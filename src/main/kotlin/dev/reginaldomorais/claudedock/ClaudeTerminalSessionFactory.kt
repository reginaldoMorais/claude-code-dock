package dev.reginaldomorais.claudedock

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import dev.reginaldomorais.claudedock.settings.ClaudeDockProjectSettings
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellStartupOptions
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
     * Cria uma sessão de terminal na raiz do projeto e executa [command] nela.
     *
     * O widget é filho de [parent], de modo que fechar a aba encerra o processo (RNF-09).
     */
    fun createSession(project: Project, parent: Disposable, command: String): TerminalWidget {
        val runner = LocalTerminalDirectRunner.createTerminalRunner(project)
        val configDir = ClaudeDockProjectSettings.getInstance(project).effectiveConfigDir()
        val options = ShellStartupOptions.Builder()
            .workingDirectory(ClaudeWorkingDirectory.resolve(project.basePath))
            .envVariables(ClaudeEnvironment.build(configDir))
            .build()

        // deferSessionStartUntilUiShown = true: o processo só nasce quando a UI aparece (RNF-02).
        val widget = runner.startShellTerminalWidget(parent, options, true)
        // Fora da tool window "Terminal" a plataforma engole o Esc (RF-17).
        ClaudeEscapeForwarder.install(widget)
        // Cópia por seleção com o mouse (RF-26); some junto com a aba.
        ClaudeSelectionCopyButton.install(widget, parent)
        // Respiro entre o conteúdo e as bordas da janela; o JediTerm só reserva 4px à esquerda.
        ClaudeSessionPadding.apply(
            widget.component,
            JBTerminalWidget.asJediTermWidget(widget)?.terminalPanel,
            ClaudeDockSettings.getInstance().effectivePadding(),
        )
        widget.sendCommandToExecute(command)
        return widget
    }

    /**
     * Conteúdo do buffer da sessão: scrollback mais a tela visível (RF-21).
     *
     * No engine CLASSIC a plataforma monta a seleção do topo do histórico até a última linha
     * da tela. Fora dele a interface cai no seu `default`, que devolve texto vazio — o botão
     * de copiar avisa em vez de quebrar (CB-26).
     */
    fun readText(widget: TerminalWidget): CharSequence = widget.getText()

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
     * Faz acesso a disco — não deve ser chamado na EDT (RNF-03).
     */
    fun isExecutableAvailable(executable: String): Boolean {
        val trimmed = executable.trim()
        if (trimmed.isEmpty()) return false

        // Caminho explícito: precisa existir e ser executável.
        if (trimmed.contains(File.separatorChar) || trimmed.contains('/')) {
            val file = File(trimmed)
            return file.isFile && file.canExecute()
        }

        return PathEnvironmentVariableUtil.findInPath(trimmed) != null
    }
}
