package dev.reginaldomorais.claudedock.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager
import dev.reginaldomorais.claudedock.ClaudeDockSessions

/**
 * Abre e foca a tool window dedicada (RF-09).
 *
 * Sem atalho padrão: `Ctrl+Esc` pertence ao plugin oficial e não deve ser sobrescrito (RF-13).
 */
class OpenClaudeDockAction : AnAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ToolWindowManager.getInstance(project)
            .getToolWindow(ClaudeDockSessions.TOOL_WINDOW_ID)
            ?.activate(null)
    }
}
