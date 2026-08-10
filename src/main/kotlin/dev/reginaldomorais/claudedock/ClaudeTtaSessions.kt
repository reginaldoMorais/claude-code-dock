package dev.reginaldomorais.claudedock

import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings
import dev.reginaldomorais.claudedock.settings.TtsEngine
import java.util.concurrent.atomic.AtomicLong

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
     * Identifica a fala em curso.
     *
     * Com streaming, [ClaudeTtsPlayback.speak] bloqueia durante toda a reprodução. Sem este
     * contador, a fala antiga — que a nova acabou de interromper — voltaria e marcaria `Idle`
     * por cima da fala nova, que já está tocando.
     */
    private val playGeneration = AtomicLong()

    /**
     * Sintetiza e toca o texto. Pede ao [ClaudeTtsPlayback] fora da EDT.
     * Novo play interrompe anterior (RNF-23).
     *
     * A reprodução **não** volta para a EDT como antes: com streaming, tocar é o próprio laço que
     * lê o stdout do motor, e ele bloqueia por toda a fala (RF-57). Só o estado é publicado.
     */
    fun playText(text: String) {
        if (text.isBlank()) return

        stop()  // Parar anterior

        val generation = playGeneration.incrementAndGet()
        state = TtsState.Playing

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val settings = ClaudeDockSettings.getInstance()

                if (!ClaudeTtsPlayback.canSynthesize(settings)) {
                    LOG.warn("TTS engine ${settings.ttsEngine} is not available for synthesis")
                    finish(generation)
                    notifyEngineMissing(settings.ttsEngine)
                    return@executeOnPooledThread
                }

                ClaudeTtsPlayback.speak(text, ClaudeTtsPlayback.buildCommand(settings))
                finish(generation)
            } catch (e: Exception) {
                LOG.warn("Failed to play text", e)
                finish(generation)
            }
        }
    }

    /** Volta para `Idle` só se ninguém tiver começado outra fala no meio-tempo. */
    private fun finish(generation: Long) {
        if (playGeneration.get() != generation) return

        state = TtsState.Idle
        notifyStateChanged()
    }

    fun pause() {
        if (state == TtsState.Playing) {
            ClaudeTtsPlayback.pause()
            state = TtsState.Paused
            notifyStateChanged()
        }
    }

    fun resume() {
        if (state == TtsState.Paused) {
            ClaudeTtsPlayback.resume()
            state = TtsState.Playing
            notifyStateChanged()
        }
    }

    fun togglePauseResume() {
        if (state == TtsState.Playing) pause() else if (state == TtsState.Paused) resume()
    }

    fun stop() {
        if (state != TtsState.Idle) {
            ClaudeTtsPlayback.stop()
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

    /**
     * Avisa que o motor de voz não está configurado (RNF-33, D-42).
     *
     * **Fica aqui, e não no chamador, de propósito.** Os dois pontos que tocam um trecho — o item
     * "Tocar seleção" do menu "Áudio" (RF-48) e o botão do popup da seleção (RF-50) — passam por
     * [playText]. Uma guarda neste ponto conserta os dois; uma em cada chamador seria o dobro do
     * código e deixaria o terceiro chamador quebrado no dia em que existir.
     *
     * O menu escondia a falta: o `update()` da ação desabilita o item quando o Piper não está
     * configurado. **O popup não tem `update()`** — o botão está sempre lá, e sem este aviso o
     * clique seria de novo o no-op mudo do DEF-07.
     */
    private fun notifyEngineMissing(engine: TtsEngine) {
        // Roda em thread de pool, e o projeto pode ter fechado no meio da verificação em disco.
        if (project.isDisposed) return

        val name = when (engine) {
            TtsEngine.PIPER -> "Piper"
            TtsEngine.KOKORO -> "Kokoro"
        }

        ClaudeDockSessions.getInstance(project).notify(
            "$name não está configurado. Ajuste os caminhos em " +
                "Settings > Tools > Claude Code Dock.",
            NotificationType.WARNING,
        )
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
