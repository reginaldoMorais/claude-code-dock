package dev.reginaldomorais.claudedock.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import dev.reginaldomorais.claudedock.ClaudeTtaSessions
import dev.reginaldomorais.claudedock.TtsState
import dev.reginaldomorais.claudedock.TtsStateListener

/**
 * Alterna entre pausar e retomar a reprodução de áudio (RF-31).
 */
class AudioPauseResumeAction : AnAction("Pausar", "Pausar ou retomar reprodução de áudio", AllIcons.Actions.Pause), DumbAware {

    private var currentState: TtsState = TtsState.Idle

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
        currentState = state

        when (state) {
            TtsState.Playing -> {
                e.presentation.text = "Pausar"
                e.presentation.icon = AllIcons.Actions.Pause
                e.presentation.isEnabled = true
            }
            TtsState.Paused -> {
                e.presentation.text = "Retomar"
                e.presentation.icon = AllIcons.Actions.Resume
                e.presentation.isEnabled = true
            }
            TtsState.Idle -> {
                e.presentation.text = "Pausar"
                e.presentation.icon = AllIcons.Actions.Pause
                e.presentation.isEnabled = false
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val sessions = ClaudeTtaSessions.getInstance(project)
        sessions.togglePauseResume()
    }
}
