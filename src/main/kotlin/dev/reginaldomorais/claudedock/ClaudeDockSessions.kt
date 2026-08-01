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

/**
 * Gerencia as abas de sessão dentro da tool window dedicada.
 *
 * Não conhece detalhes da API de terminal — isso é responsabilidade de
 * [ClaudeTerminalSessionFactory] (RNF-14).
 */
@Service(Service.Level.PROJECT)
class ClaudeDockSessions(private val project: Project) {

    /** Guarda contra dois `/export` simultâneos na mesma janela (CB-32). */
    private val exportInProgress = AtomicBoolean(false)

    /** Sessão nova (RF-07). Único ponto que monta o comando, para não duplicar a regra. */
    fun openNewSession() = ClaudeDockSettings.getInstance().let {
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

        // Disposable da aba: fechar a aba encerra o processo e libera o PTY (RNF-09).
        val sessionDisposable = Disposer.newDisposable("ClaudeDockSession")

        val widget = try {
            ClaudeTerminalSessionFactory.createSession(project, sessionDisposable, command)
        } catch (e: Exception) {
            Disposer.dispose(sessionDisposable)
            LOG.warn("Falha ao criar a sessão do Claude Code", e)
            notify("Não foi possível iniciar a sessão: ${e.message}", NotificationType.ERROR)
            return
        }

        val content: Content = ContentFactory.getInstance()
            .createContent(widget.component, title, false)
        content.isCloseable = true
        content.setDisposer(sessionDisposable)
        content.preferredFocusableComponent = widget.component
        // A referência vive junto com a aba: fechar a aba a leva embora, sem mapa para limpar.
        content.putUserData(SESSION_WIDGET, widget)

        contentManager.addContent(content)
        contentManager.setSelectedContent(content)

        // Sessão encerrada apenas marca a aba; o scrollback é preservado (RF-11).
        widget.addTerminationCallback({
            ApplicationManager.getApplication().invokeLater {
                if (!project.isDisposed) {
                    content.displayName = "$title (encerrado)"
                }
            }
        }, sessionDisposable)
    }

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

    private fun notify(message: String, type: NotificationType) {
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

        /** Liga a aba ao seu widget, para as ações que operam sobre a sessão selecionada. */
        private val SESSION_WIDGET = Key.create<TerminalWidget>("ClaudeDockSessionWidget")

        private val LOG = Logger.getInstance(ClaudeDockSessions::class.java)

        fun getInstance(project: Project): ClaudeDockSessions = project.service()
    }
}
