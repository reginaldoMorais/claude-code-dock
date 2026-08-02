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
class AudioPauseResumeAction : AnAction(), DumbAware {

    private var currentState: TtsState = TtsState.Idle

    init {
        val project = null // Will be set in update()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project ?: run {
            e.presentation.isEnabled = false
            return
        }

        val sessions = ClaudeTtaSessions.getInstance(project)
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
