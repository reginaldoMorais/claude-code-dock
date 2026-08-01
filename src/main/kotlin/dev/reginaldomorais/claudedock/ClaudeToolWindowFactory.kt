package dev.reginaldomorais.claudedock

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.SimpleTextAttributes
import dev.reginaldomorais.claudedock.actions.NewSessionAction
import dev.reginaldomorais.claudedock.actions.ResumeSessionAction
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings

/**
 * Registra a tool window dedicada e cria a primeira sessão.
 *
 * Não sabe como um terminal é criado — delega a [ClaudeDockSessions] (RNF-14).
 * A criação é preguiçosa: só ocorre quando o usuário abre a janela (RF-05, RNF-01).
 */
class ClaudeToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setTitleActions(listOf(NewSessionAction(), ResumeSessionAction()))
        installEmptyState(project, toolWindow)

        val command = ClaudeCommand.newSession(ClaudeDockSettings.getInstance().effectiveExecutable())
        ClaudeDockSessions.getInstance(project).addSession(toolWindow.contentManager, command)
    }

    /**
     * Torna utilizável o estado sem abas (RF-18).
     *
     * `createToolWindowContent` roda uma única vez: ao fechar a última aba a janela ficaria
     * com o "Nothing to show" padrão e sem saída visível. Os links reaproveitam as mesmas
     * ações do cabeçalho.
     */
    private fun installEmptyState(project: Project, toolWindow: ToolWindow) {
        val emptyText = (toolWindow as? ToolWindowEx)?.emptyText ?: return
        val sessions = ClaudeDockSessions.getInstance(project)

        emptyText.setText("Nenhuma sessão do Claude Code aberta")
        emptyText.appendLine("Nova sessão", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
            sessions.openNewSession()
        }
        emptyText.appendLine("Retomar sessão", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
            sessions.openResumeSession()
        }
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
