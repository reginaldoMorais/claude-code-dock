package dev.reginaldomorais.claudedock.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import dev.reginaldomorais.claudedock.ClaudeDockSessions

/**
 * Retoma uma conversa anterior via `claude --resume` (RF-08).
 *
 * O histórico é responsabilidade do CLI; o plugin não persiste nada (D-02).
 */
class ResumeSessionAction : AnAction(
    "Retomar Sessão",
    "Retoma uma conversa anterior do Claude Code",
    AllIcons.Actions.Rerun,
), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeDockSessions.getInstance(project).openResumeSession()
    }
}
