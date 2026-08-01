package dev.reginaldomorais.claudedock

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import dev.reginaldomorais.claudedock.settings.ClaudeDockConfigurable
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings

/**
 * Gerencia as abas de sessão dentro da tool window dedicada.
 *
 * Não conhece detalhes da API de terminal — isso é responsabilidade de
 * [ClaudeTerminalSessionFactory] (RNF-14).
 */
@Service(Service.Level.PROJECT)
class ClaudeDockSessions(private val project: Project) {

    /** Sessão nova (RF-07). Único ponto que monta o comando, para não duplicar a regra. */
    fun openNewSession() =
        openSession(ClaudeCommand.newSession(ClaudeDockSettings.getInstance().effectiveExecutable()))

    /** Retomada de conversa anterior (RF-08). */
    fun openResumeSession() =
        openSession(ClaudeCommand.resumeSession(ClaudeDockSettings.getInstance().effectiveExecutable()))

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

        private val LOG = Logger.getInstance(ClaudeDockSessions::class.java)

        fun getInstance(project: Project): ClaudeDockSessions = project.service()
    }
}
