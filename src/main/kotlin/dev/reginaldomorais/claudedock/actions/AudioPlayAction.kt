package dev.reginaldomorais.claudedock.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import dev.reginaldomorais.claudedock.ClaudeDockSessions
import dev.reginaldomorais.claudedock.ClaudeTtsPlayback
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings

/**
 * Toca o texto selecionado na sessão ativa (RF-48).
 */
class AudioPlayAction : AnAction("Tocar seleção", "Tocar texto selecionado em voz alta", AllIcons.Actions.Execute), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val settings = ClaudeDockSettings.getInstance()
        e.presentation.isEnabled = e.project != null && ClaudeTtsPlayback.canSynthesize(settings)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeDockSessions.getInstance(project).playSelectedSession()
    }
}
