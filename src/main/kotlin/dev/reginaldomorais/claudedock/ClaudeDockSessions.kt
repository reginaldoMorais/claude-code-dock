package dev.reginaldomorais.claudedock

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import dev.reginaldomorais.claudedock.settings.ClaudeDockConfigurable
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * Gerencia as abas de sessão dentro da tool window dedicada.
 *
 * Não conhece detalhes da API de terminal — isso é responsabilidade de
 * [ClaudeTerminalSessionFactory] (RNF-14).
 */
@Service(Service.Level.PROJECT)
class ClaudeDockSessions(private val project: Project) :
    ClaudeTerminalSessionFactory.SessionHost {

    /** Guarda contra dois `/export` simultâneos na mesma janela (CB-32). */
    private val exportInProgress = AtomicBoolean(false)

    /** Sessão nova (RF-07). Único ponto que monta o comando, para não duplicar a regra. */
    override fun openNewSession() = ClaudeDockSettings.getInstance().let {
        openSession(ClaudeCommand.newSession(it.effectiveExecutable()))
    }

    /** Retomada de conversa anterior (RF-08). */
    fun openResumeSession() = ClaudeDockSettings.getInstance().let {
        openSession(ClaudeCommand.resumeSession(it.effectiveExecutable()))
    }

    /**
     * Abre uma sessão em nova aba, ativando a tool window.
     *
     * A verificação do executável roda fora da EDT (RNF-03); a sessão é aberta de
     * qualquer forma, deixando um shell utilizável para diagnóstico (Fluxo D).
     */
    fun openSession(command: String) {
        val executable = ClaudeDockSettings.getInstance().effectiveExecutable()

        ApplicationManager.getApplication().executeOnPooledThread {
            val available = ClaudeTerminalSessionFactory.isExecutableAvailable(executable)

            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                if (!available) notifyExecutableMissing(executable)

                val toolWindow = findToolWindow() ?: return@invokeLater
                toolWindow.activate {
                    addSession(toolWindow.contentManager, command)
                }
            }
        }
    }

    /**
     * Cria a aba de sessão e a adiciona ao [contentManager].
     *
     * Falha ao criar não derruba a tool window (RNF-10).
     */
    fun addSession(contentManager: ContentManager, command: String) {
        val title = nextTabTitle(contentManager)

        // Disposable da aba: fechar a aba encerra **todas** as suas sessões (RNF-09, RNF-28).
        val tabDisposable = Disposer.newDisposable("ClaudeDockTab")

        val pane = createPane(tabDisposable, command) ?: run {
            Disposer.dispose(tabDisposable)
            return
        }

        val content: Content = ContentFactory.getInstance()
            // O painel raiz é o que dá onde encaixar o splitter na primeira divisão (RF-36).
            .createContent(ClaudeSessionSplitter.root(pane.component), title, false)
        content.isCloseable = true
        content.setDisposer(tabDisposable)
        // O foco vai para o terminal, e não para o painel que o cobre.
        content.preferredFocusableComponent = pane.widget.component
        // A referência vive junto com a aba: fechar a aba a leva embora, sem mapa para limpar.
        content.putUserData(SESSION_WIDGET, pane.widget)

        contentManager.addContent(content)
        contentManager.setSelectedContent(content)

        // Sessão encerrada apenas marca a aba; o scrollback é preservado (RF-11).
        // ponytail: só a sessão original renomeia a aba. Com split, marcar a aba porque *uma*
        // pane morreu diria menos do que parece — ver Q-26.
        pane.widget.addTerminationCallback({
            ApplicationManager.getApplication().invokeLater {
                if (!project.isDisposed) {
                    content.displayName = "$title (encerrado)"
                }
            }
        }, tabDisposable)
    }

    /**
     * Divide a pane de [widget], abrindo uma sessão nova ao lado (RF-36).
     *
     * A nova pane é filha do disposable da **aba**, e não da pane de origem: fechar a divisão de
     * cima não pode arrastar a de baixo junto (RNF-28).
     */
    override fun splitSession(widget: TerminalWidget, stacked: Boolean) {
        val content = contentOf(widget)
            ?: return notify("Nenhuma sessão aberta para dividir.", NotificationType.WARNING)
        val tabDisposable = content.disposer ?: return
        val existing = ClaudeSessionSplitter.paneOf(widget.component, content.component) ?: return

        val command = ClaudeCommand.newSession(
            ClaudeDockSettings.getInstance().effectiveExecutable(),
        )
        val incoming = createPane(tabDisposable, command) ?: return

        if (!ClaudeSessionSplitter.split(existing, incoming.component, stacked)) {
            Disposer.dispose(incoming.disposable)
            return notify("Não foi possível dividir a sessão.", NotificationType.WARNING)
        }

        // A divisão nasce com o foco: quem dividiu quer digitar na sessão nova.
        incoming.widget.requestFocus()
    }

    /** Divide a sessão em foco da aba selecionada — o caminho do cabeçalho (RF-36). */
    fun splitSelectedSession(stacked: Boolean) {
        val widget = selectedWidget()
            ?: return notify("Nenhuma sessão aberta para dividir.", NotificationType.WARNING)

        splitSession(widget, stacked)
    }

    /**
     * Fecha a pane de [widget], colapsando a divisão (RF-39).
     *
     * Sendo a única sessão da aba, fechar a sessão é fechar a aba — que é o que o item "Close
     * Tab" do menu de contexto significa quando não há divisão.
     */
    override fun closeSession(widget: TerminalWidget) {
        if (closeSplit(widget)) return

        contentOf(widget)?.let { findToolWindow()?.contentManager?.removeContent(it, true) }
    }

    /**
     * Fecha a divisão em foco pelo cabeçalho, **sem** fechar a aba (RF-41).
     *
     * O menu de contexto do terminal já faz isso desde v1.7, mas sob o rótulo "Close Tab", que
     * é da plataforma e sugere o oposto do que faz numa aba dividida. Daí o item próprio.
     */
    fun closeSelectedSplit() {
        val widget = selectedWidget()
            ?: return notify("Nenhuma sessão aberta para fechar.", NotificationType.WARNING)

        if (!closeSplit(widget)) {
            notify("Esta aba não está dividida; feche a aba pelo X.", NotificationType.INFORMATION)
        }
    }

    /**
     * Tira a pane de [widget] da árvore e encerra **só** o processo dela (RNF-28).
     *
     * @return `false` quando não havia divisão a fechar.
     */
    private fun closeSplit(widget: TerminalWidget): Boolean {
        val content = contentOf(widget) ?: return false
        val pane = ClaudeSessionSplitter.paneOf(widget.component, content.component) ?: return false
        val sibling = ClaudeSessionSplitter.close(pane) ?: return false

        // A pane saiu da árvore; o processo dela morre aqui, e só ele.
        paneDisposable(pane)?.let(Disposer::dispose)

        // Sem isto a aba fica apontando para uma sessão morta, e as ações do cabeçalho param de
        // achar sessão até o usuário clicar em alguma pane.
        firstWidget(sibling)?.let { survivor ->
            content.putUserData(SESSION_WIDGET, survivor)
            survivor.requestFocus()
        }

        return true
    }

    /** Primeira sessão viva sob [component] — usada para reassumir o foco após um fechamento. */
    private fun firstWidget(component: JComponent): TerminalWidget? {
        (component.getClientProperty(PANE_WIDGET) as? TerminalWidget)?.let { return it }

        return component.components
            .filterIsInstance<JComponent>()
            .firstNotNullOfOrNull { firstWidget(it) }
    }

    /**
     * Navegação entre abas pedida pelo menu de contexto do terminal.
     *
     * A guarda não é defensiva por precaução: com uma aba só, a plataforma dispara assertion em
     * vez de ignorar o pedido (DEF-02). E o menu mostra os itens sempre, sem nos consultar.
     */
    override fun selectSiblingTab(next: Boolean) {
        val contentManager = findToolWindow()?.contentManager ?: return
        if (!ClaudeTabNavigation.canNavigate(contentManager.contentCount)) return

        if (next) contentManager.selectNextContent() else contentManager.selectPreviousContent()
    }

    /**
     * Registra qual sessão está em foco na aba (RF-38, D-34).
     *
     * Com o split, uma aba tem várias sessões, e as ações do cabeçalho precisam saber sobre qual
     * agir. A chave é a **mesma** de antes: passa a significar "a última sessão com foco nesta
     * aba" em vez de "a única sessão da aba", e por isso `selectedWidget()` não mudou.
     */
    override fun sessionFocused(widget: TerminalWidget) {
        contentOf(widget)?.putUserData(SESSION_WIDGET, widget)
    }

    /**
     * Cria uma sessão e o componente que a representa na árvore da aba.
     *
     * Falha ao criar não derruba a tool window nem a aba (RNF-10): notifica e devolve `null`.
     */
    private fun createPane(tabDisposable: Disposable, command: String): Pane? {
        val paneDisposable = Disposer.newDisposable("ClaudeDockPane")
        Disposer.register(tabDisposable, paneDisposable)

        val widget = try {
            ClaudeTerminalSessionFactory.createSession(project, paneDisposable, command, this)
        } catch (e: Exception) {
            Disposer.dispose(paneDisposable)
            LOG.warn("Falha ao criar a sessão do Claude Code", e)
            notify("Não foi possível iniciar a sessão: ${e.message}", NotificationType.ERROR)
            return null
        }

        // Esconde o prompt e o eco do comando enquanto o CLI sobe.
        val component = ClaudeSessionLoading.wrap(
            widget.component,
            paneDisposable,
            "Iniciando o Claude Code…",
        )
        // Propriedades de cliente em vez de mapa: morrem junto com o componente (D-18, D-36).
        component.putClientProperty(PANE_DISPOSABLE, paneDisposable)
        component.putClientProperty(PANE_WIDGET, widget)

        return Pane(widget, component, paneDisposable)
    }

    private fun paneDisposable(pane: JComponent): Disposable? =
        pane.getClientProperty(PANE_DISPOSABLE) as? Disposable

    /** Aba que contém [widget], descoberta pela árvore de componentes — sem mapa a manter. */
    private fun contentOf(widget: TerminalWidget): Content? =
        findToolWindow()?.contentManager?.contents?.firstOrNull {
            SwingUtilities.isDescendingFrom(widget.component, it.component)
        }

    /** Uma sessão e o bloco que a representa na aba. */
    private class Pane(
        val widget: TerminalWidget,
        val component: JComponent,
        val disposable: Disposable,
    )

    /**
     * Copia a conversa da aba selecionada para a área de transferência (RF-24).
     *
     * Passa pelo `/export` do CLI, e não pelo buffer do terminal: o buffer traz a conversa
     * repetida, uma cópia por repintura do TUI (DEF-01). Silencioso no sucesso, como qualquer
     * botão de copiar do IDE.
     */
    fun copySelectedSession() {
        val widget = selectedWidget()
            ?: return notify("Nenhuma sessão aberta para copiar.", NotificationType.WARNING)

        // Um /export por vez: dois em paralelo disputariam a mesma sessão (CB-32).
        if (!exportInProgress.compareAndSet(false, true)) {
            return notify("Já há uma cópia em andamento.", NotificationType.INFORMATION)
        }

        val target = try {
            ClaudeSessionExport.createTarget()
        } catch (e: IOException) {
            exportInProgress.set(false)
            LOG.warn("Não foi possível criar o arquivo temporário do export", e)
            return notify("Não foi possível preparar a cópia: ${e.message}", NotificationType.ERROR)
        }

        if (!ClaudeTerminalSessionFactory.sendInput(widget, ClaudeSessionExport.command(target))) {
            ClaudeSessionExport.delete(target)
            exportInProgress.set(false)
            return notify("A sessão ainda não iniciou; tente de novo em instantes.", NotificationType.WARNING)
        }

        // A espera é por disco: nunca na EDT (RNF-03).
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val text = ClaudeSessionExport.awaitContent(target)

                ApplicationManager.getApplication().invokeLater {
                    if (project.isDisposed) return@invokeLater
                    if (text == null) {
                        notify(
                            "Não foi possível obter a conversa. A sessão está respondendo?",
                            NotificationType.WARNING,
                        )
                    } else {
                        CopyPasteManager.copyTextToClipboard(text)
                    }
                }
            } finally {
                // O arquivo carrega a conversa: some com ou sem sucesso (RF-25, R-14).
                ClaudeSessionExport.delete(target)
                exportInProgress.set(false)
            }
        }
    }

    /**
     * Pede ao próprio CLI a transcrição da conversa, via slash command `/export` (RF-22).
     *
     * O CR final equivale ao Enter: quem apresenta as opções de destino é o Claude Code.
     */
    fun exportSelectedSession() {
        val widget = selectedWidget()
            ?: return notify("Nenhuma sessão aberta para exportar.", NotificationType.WARNING)

        if (!ClaudeTerminalSessionFactory.sendInput(widget, EXPORT_COMMAND)) {
            notify("A sessão ainda não iniciou; tente de novo em instantes.", NotificationType.WARNING)
        }
    }

    private fun selectedWidget(): TerminalWidget? =
        findToolWindow()?.contentManager?.selectedContent?.getUserData(SESSION_WIDGET)

    private fun findToolWindow(): ToolWindow? =
        ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)

    private fun nextTabTitle(contentManager: ContentManager): String =
        ClaudeTabTitle.next(contentManager.contents.mapNotNull { it.displayName }.toSet())

    private fun notifyExecutableMissing(executable: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(
                "Claude Code não encontrado",
                "Não foi possível localizar o executável \"$executable\". " +
                    "Configure o caminho completo nas configurações do plugin.",
                NotificationType.WARNING,
            )
            .addAction(object : com.intellij.openapi.actionSystem.AnAction("Abrir Configurações") {
                override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                    ShowSettingsUtil.getInstance()
                        .showSettingsDialog(project, ClaudeDockConfigurable::class.java)
                }
            })
            .notify(project)
    }

    /** `internal` para o popup de seleção reusar o mesmo grupo, em vez de duplicar a chamada. */
    internal fun notify(message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification("Claude Code Dock", message, type)
            .notify(project)
    }

    companion object {
        const val TOOL_WINDOW_ID = "Claude Code Dock"
        const val NOTIFICATION_GROUP = "ClaudeCodeDock"

        /** Slash command do CLI, seguido de CR — o mesmo que digitar e pressionar Enter. */
        private const val EXPORT_COMMAND = "/export\r"

        /**
         * Liga a aba à sessão em foco, para as ações que operam sobre a sessão selecionada.
         *
         * _(v1.7)_ Com o split, uma aba tem várias sessões: a chave passou a significar "a
         * última com foco" e é reescrita por [sessionFocused].
         */
        private val SESSION_WIDGET = Key.create<TerminalWidget>("ClaudeDockSessionWidget")

        /** Disposable da pane, pendurado no próprio componente (D-18). */
        private const val PANE_DISPOSABLE = "ClaudeDockPaneDisposable"

        /** Sessão da pane, pendurada no mesmo lugar — permite achar quem sobreviveu (RF-41). */
        private const val PANE_WIDGET = "ClaudeDockPaneWidget"

        private val LOG = Logger.getInstance(ClaudeDockSessions::class.java)

        fun getInstance(project: Project): ClaudeDockSessions = project.service()
    }
}
