package dev.reginaldomorais.claudedock.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import kotlin.math.abs

/** Motores de voz suportados (RF-54). */
enum class TtsEngine { PIPER, KOKORO }

/**
 * Configuração persistida do plugin.
 *
 * Guarda apenas o caminho do executável. Nenhum segredo é armazenado (RNF-05).
 */
@State(
    name = "ClaudeDockSettings",
    storages = [Storage("claude-code-dock.xml")],
)
class ClaudeDockSettings : PersistentStateComponent<ClaudeDockSettings> {

    /** Caminho do CLI. O padrão resolve pelo PATH. */
    var claudeExecutable: String = DEFAULT_EXECUTABLE

    /** Respiro entre o conteúdo da sessão e as bordas da janela, em pixels lógicos. */
    var sessionPadding: Int = DEFAULT_PADDING

    /** Tecla que representa Esc (mantida por compatibilidade, sempre Backspace agora). */
    @Deprecated("Sempre Backspace em GoLand; Esc em IntelliJ")
    var escapeKeyName: String = "BACKSPACE"

    /** Motor de voz em uso. Kokoro é o padrão desde a v1.11 (RF-54). */
    var ttsEngine: TtsEngine = TtsEngine.KOKORO

    /** Caminho do Piper TTS. O padrão resolve pelo PATH. */
    var piperExecutable: String = "piper"

    /** Caminho absoluto para o arquivo .onnx do modelo de voz. Vazio desabilita síntese. */
    var piperModel: String = ""

    /** Interpretador Python com `kokoro-onnx` instalado. O padrão resolve pelo PATH. */
    var kokoroPython: String = DEFAULT_KOKORO_PYTHON

    /** Caminho absoluto do `kokoro-v1.0.onnx`. Vazio desabilita síntese. */
    var kokoroModel: String = ""

    /** Caminho absoluto do `voices-v1.0.bin`. Vazio desabilita síntese. */
    var kokoroVoices: String = ""

    /** Voz do Kokoro, como aparece no arquivo de vozes. */
    var kokoroVoice: String = DEFAULT_KOKORO_VOICE

    /** Velocidade da fala em porcentagem, sempre uma das oferecidas em [SPEECH_SPEEDS]. */
    var speechSpeed: Int = DEFAULT_SPEECH_SPEED


    override fun getState(): ClaudeDockSettings = this

    override fun loadState(state: ClaudeDockSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    /** Caminho efetivo: cai no padrão se o usuário limpar o campo. */
    fun effectiveExecutable(): String =
        claudeExecutable.trim().ifEmpty { DEFAULT_EXECUTABLE }

    /**
     * Respiro efetivo, limitado à faixa aceita.
     *
     * O spinner da tela já limita, mas o XML é editável à mão: um valor absurdo aqui comeria
     * as colunas do terminal.
     */
    fun effectivePadding(): Int = sessionPadding.coerceIn(MIN_PADDING, MAX_PADDING)

    /** Executável do Piper: cai no padrão se o usuário limpar o campo. */
    fun effectivePiperExecutable(): String =
        piperExecutable.trim().ifEmpty { "piper" }

    /** Caminho do modelo: vazio significa síntese desabilitada. */
    fun effectivePiperModel(): String = piperModel.trim()

    /** Interpretador do Kokoro: cai no padrão se o usuário limpar o campo. */
    fun effectiveKokoroPython(): String =
        kokoroPython.trim().ifEmpty { DEFAULT_KOKORO_PYTHON }

    /** Caminho do modelo: vazio significa síntese desabilitada. */
    fun effectiveKokoroModel(): String = kokoroModel.trim()

    /** Caminho do arquivo de vozes: vazio significa síntese desabilitada. */
    fun effectiveKokoroVoices(): String = kokoroVoices.trim()

    /** Voz do Kokoro: cai no padrão se o usuário limpar o campo. */
    fun effectiveKokoroVoice(): String =
        kokoroVoice.trim().ifEmpty { DEFAULT_KOKORO_VOICE }

    /**
     * Velocidade efetiva: sempre uma das oferecidas, a mais próxima do que está gravado.
     *
     * Menu e tela só oferecem a tabela, mas o XML é editável à mão. Aproximar, em vez de apenas
     * limitar a faixa, é o que garante que o seletor da tela nunca fique sem item selecionado.
     */
    fun effectiveSpeechSpeed(): Int = SPEECH_SPEEDS.keys.minByOrNull { abs(it - speechSpeed) }!!

    companion object {
        const val DEFAULT_EXECUTABLE = "claude"
        const val DEFAULT_PADDING = 20
        const val MIN_PADDING = 0
        const val MAX_PADDING = 48
        const val DEFAULT_SPEECH_SPEED = 100
        const val DEFAULT_KOKORO_PYTHON = "python3"
        const val DEFAULT_KOKORO_VOICE = "pf_dora"

        /**
         * Velocidades oferecidas, na ordem de exibição.
         *
         * Fonte única do menu "Áudio" e da tela de configuração — é o que impede os dois de
         * divergirem. Mora aqui, e não nas actions, porque é o domínio do valor.
         *
         * 0,25x saiu na v1.11: o `create()` do Kokoro tem `assert speed >= 0.5`, e a tabela é
         * compartilhada pelos dois motores (D-45). Quem tinha 25 gravado migra sozinho pelo
         * [effectiveSpeechSpeed], que aproxima em vez de apenas limitar a faixa.
         */
        val SPEECH_SPEEDS: Map<Int, String> = linkedMapOf(
            50 to "0,5x",
            75 to "0,75x",
            100 to "1x (padrão do modelo)",
            125 to "1,25x",
            150 to "1,5x",
            175 to "1,75x",
            200 to "2x",
        )

        /** Rótulo da velocidade. Fora da tabela só acontece com XML editado à mão. */
        fun speechSpeedLabel(percent: Int): String = SPEECH_SPEEDS[percent] ?: "$percent%"

        fun getInstance(): ClaudeDockSettings =
            ApplicationManager.getApplication().getService(ClaudeDockSettings::class.java)
    }
}
