package dev.reginaldomorais.claudedock.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.IconLoader
import dev.reginaldomorais.claudedock.ClaudeTtsPlayback
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings
import dev.reginaldomorais.claudedock.ClaudeToolWindowFactory

/**
 * Menu "Áudio" com tooltip dinâmico que informa quando o motor de voz não está disponível.
 */
class AudioMenuAction : DefaultActionGroup("Áudio", true) {

    init {
        templatePresentation.icon = IconLoader.getIcon("/icons/audio-wave.svg", ClaudeToolWindowFactory::class.java)
        add(AudioPlayAction())
        addSeparator()
        add(AudioPauseResumeAction())
        add(AudioStopAction())
        addSeparator()
        add(SpeechSpeedMenuAction())
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val settings = ClaudeDockSettings.getInstance()

        if (ClaudeTtsPlayback.canSynthesize(settings)) {
            e.presentation.text = "Áudio"
            e.presentation.description = "Tocar, pausar e parar a leitura em voz alta"
            e.presentation.isEnabled = true
        } else {
            e.presentation.text = "Áudio (voz não configurada)"
            e.presentation.description = "Configure o motor de voz em Settings > Tools > Claude Code Dock"
            e.presentation.isEnabled = false
        }
    }
}
