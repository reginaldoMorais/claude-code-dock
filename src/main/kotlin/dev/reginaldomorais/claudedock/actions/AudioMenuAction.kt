package dev.reginaldomorais.claudedock.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.IconLoader
import dev.reginaldomorais.claudedock.ClaudePiperPlayback
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings
import dev.reginaldomorais.claudedock.ClaudeToolWindowFactory

/**
 * Menu "Áudio" com tooltip dinâmico que informa quando Piper não está disponível.
 */
class AudioMenuAction : DefaultActionGroup("Áudio", true) {

    companion object {
        // Ativa para testar o tooltip "não instalado". Desativa para uso normal.
        private const val TEST_PIPER_UNAVAILABLE = false
    }

    init {
        templatePresentation.icon = IconLoader.getIcon("/icons/audio-wave.svg", ClaudeToolWindowFactory::class.java)
        add(AudioPlayAction())
        addSeparator()
        add(AudioPauseResumeAction())
        add(AudioStopAction())
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val settings = ClaudeDockSettings.getInstance()
        var canPlay = ClaudePiperPlayback.canSynthesize(
            settings.effectivePiperExecutable(),
            settings.effectivePiperModel(),
        )

        // Para teste: força simulação de Piper não instalado
        if (TEST_PIPER_UNAVAILABLE) canPlay = false

        if (canPlay) {
            e.presentation.text = "Áudio"
            e.presentation.description = "Tocar, pausar e parar áudio via Piper"
            e.presentation.isEnabled = true
        } else {
            e.presentation.text = "Áudio (Piper não configurado)"
            e.presentation.description = "Configure o executável e modelo do Piper em Settings"
            e.presentation.isEnabled = false
        }
    }
}
