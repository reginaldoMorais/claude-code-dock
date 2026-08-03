package dev.reginaldomorais.claudedock.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import dev.reginaldomorais.claudedock.ClaudeTtaSessions
import dev.reginaldomorais.claudedock.TtsState

/**
 * Para a reprodução de áudio (RF-31).
 */
class AudioStopAction : AnAction("Parar", "Parar reprodução de áudio", AllIcons.Actions.Suspend),
    DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        val sessions = try {
            ClaudeTtaSessions.getInstance(project)
        } catch (ex: Exception) {
            e.presentation.isEnabled = false
            return
        }

        val state = sessions.getState()
        e.presentation.isEnabled = state != TtsState.Idle
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val sessions = ClaudeTtaSessions.getInstance(project)
        sessions.stop()
    }
}
