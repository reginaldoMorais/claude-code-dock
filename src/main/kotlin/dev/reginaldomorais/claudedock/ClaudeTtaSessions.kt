package dev.reginaldomorais.claudedock

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings

/**
 * Serviço de projeto: gerencia estado único de reprodução (RNF-23).
 *
 * Uma única síntese/reprodução ativa por vez; novo play interrompe anterior.
 * Dispara eventos para listeners (ações do menu "Áudio").
 */
@Service(Service.Level.PROJECT)
class ClaudeTtaSessions(private val project: Project) {

    private val LOG = Logger.getInstance(ClaudeTtaSessions::class.java)
    private var state: TtsState = TtsState.Idle

    /**
     * Sintetiza e toca o texto. Pede ao ClaudePiperPlayback fora da EDT.
     * Novo play interrompe anterior (RNF-23).
     */
    fun playText(text: String) {
        if (text.isBlank()) return

        stop()  // Parar anterior

        state = TtsState.Playing

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val settings = ClaudeDockSettings.getInstance()
                val executable = settings.effectivePiperExecutable()
                val model = settings.effectivePiperModel()
                val speed = settings.effectiveSpeechSpeed()

                if (!ClaudePiperPlayback.canSynthesize(executable, model)) {
                    LOG.warn("Piper not available for synthesis")
                    state = TtsState.Idle
                    notifyStateChanged()
                    return@executeOnPooledThread
                }

                val pcm = ClaudePiperPlayback.synthesize(text, executable, model, speed)
                if (pcm == null) {
                    state = TtsState.Idle
                    notifyStateChanged()
                    return@executeOnPooledThread
                }

                // Voltar para EDT para tocar
                ApplicationManager.getApplication().invokeLater {
                    if (ClaudePiperPlayback.playBytes(pcm)) {
                        state = TtsState.Playing
                    } else {
                        state = TtsState.Idle
                    }
                    notifyStateChanged()
                }
            } catch (e: Exception) {
                LOG.warn("Failed to play text", e)
                state = TtsState.Idle
                ApplicationManager.getApplication().invokeLater { notifyStateChanged() }
            }
        }
    }

    fun pause() {
        if (state == TtsState.Playing) {
            ClaudePiperPlayback.pause()
            state = TtsState.Paused
            notifyStateChanged()
        }
    }

    fun resume() {
        if (state == TtsState.Paused) {
            ClaudePiperPlayback.resume()
            state = TtsState.Playing
            notifyStateChanged()
        }
    }

    fun togglePauseResume() {
        if (state == TtsState.Playing) pause() else if (state == TtsState.Paused) resume()
    }

    fun stop() {
        if (state != TtsState.Idle) {
            ClaudePiperPlayback.stop()
            state = TtsState.Idle
            notifyStateChanged()
        }
    }

    fun getState(): TtsState = state

    fun isPlaying(): Boolean = state == TtsState.Playing

    fun isPaused(): Boolean = state == TtsState.Paused

    private fun notifyStateChanged() {
        project.messageBus.syncPublisher(TtsStateListener.TOPIC).stateChanged(state)
    }

    companion object {
        fun getInstance(project: Project): ClaudeTtaSessions = project.service()
    }
}

enum class TtsState {
    Idle, Playing, Paused
}

/**
 * Listener para mudanças de estado de reprodução.
 * Usado por ações do menu "Áudio" para atualizar enable/disable e ícones.
 */
interface TtsStateListener {
    fun stateChanged(state: TtsState)

    companion object {
        val TOPIC = Topic.create("TtsStateListener", TtsStateListener::class.java)
    }
}
