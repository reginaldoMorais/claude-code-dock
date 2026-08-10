package dev.reginaldomorais.claudedock.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-1.1 e T-1.2: valor padrão, persistência e caminho efetivo. */
class ClaudeDockSettingsTest {

    @Test
    fun `valor padrao resolve pelo PATH`() {
        assertEquals("claude", ClaudeDockSettings().claudeExecutable)
    }

    @Test
    fun `estado sobrevive a copia via loadState`() {
        val source = ClaudeDockSettings().apply { claudeExecutable = "/opt/claude/bin/claude" }
        val target = ClaudeDockSettings()

        target.loadState(source.state)

        assertEquals("/opt/claude/bin/claude", target.claudeExecutable)
    }

    @Test
    fun `campo em branco volta ao padrao`() {
        val settings = ClaudeDockSettings().apply { claudeExecutable = "   " }
        assertEquals("claude", settings.effectiveExecutable())
    }

    @Test
    fun `espacos ao redor sao removidos`() {
        val settings = ClaudeDockSettings().apply { claudeExecutable = "  /opt/claude  " }
        assertEquals("/opt/claude", settings.effectiveExecutable())
    }

    @Test
    fun `respiro nasce no padrao e persiste`() {
        assertEquals(ClaudeDockSettings.DEFAULT_PADDING, ClaudeDockSettings().sessionPadding)

        val source = ClaudeDockSettings().apply { sessionPadding = 32 }
        val target = ClaudeDockSettings()

        target.loadState(source.state)

        assertEquals(32, target.effectivePadding())
    }

    /** O XML é editável à mão: valor fora da faixa não pode comer as colunas do terminal. */
    @Test
    fun `respiro fora da faixa e limitado`() {
        assertEquals(
            ClaudeDockSettings.MAX_PADDING,
            ClaudeDockSettings().apply { sessionPadding = 9999 }.effectivePadding(),
        )
        assertEquals(
            ClaudeDockSettings.MIN_PADDING,
            ClaudeDockSettings().apply { sessionPadding = -10 }.effectivePadding(),
        )
    }

    /** T-1.48: velocidade nasce no padrão e sobrevive ao loadState. */
    @Test
    fun `velocidade nasce no padrao e persiste`() {
        assertEquals(ClaudeDockSettings.DEFAULT_SPEECH_SPEED, ClaudeDockSettings().speechSpeed)

        val source = ClaudeDockSettings().apply { speechSpeed = 150 }
        val target = ClaudeDockSettings()

        target.loadState(source.state)

        assertEquals(150, target.effectiveSpeechSpeed())
    }

    /**
     * T-1.49: o XML é editável à mão, e o seletor da tela não pode ficar sem item selecionado —
     * por isso aproxima em vez de só limitar a faixa (CB-59).
     */
    @Test
    fun `velocidade fora da tabela vira a mais proxima`() {
        assertEquals(200, ClaudeDockSettings().apply { speechSpeed = 9999 }.effectiveSpeechSpeed())
        assertEquals(50, ClaudeDockSettings().apply { speechSpeed = 0 }.effectiveSpeechSpeed())
        assertEquals(100, ClaudeDockSettings().apply { speechSpeed = 110 }.effectiveSpeechSpeed())
        assertEquals(125, ClaudeDockSettings().apply { speechSpeed = 130 }.effectiveSpeechSpeed())
    }

    /** T-1.54: a tabela é a fonte única do menu e da tela — as duas UIs não podem divergir. */
    @Test
    fun `tabela de velocidades cobre de 0,5x a 2x`() {
        assertEquals(
            listOf(50, 75, 100, 125, 150, 175, 200),
            ClaudeDockSettings.SPEECH_SPEEDS.keys.toList(),
        )
        assertEquals("0,5x", ClaudeDockSettings.speechSpeedLabel(50))
        assertEquals("1,75x", ClaudeDockSettings.speechSpeedLabel(175))
        assertEquals("110%", ClaudeDockSettings.speechSpeedLabel(110))
    }

    /**
     * T-1.83 (D-45): quem já tinha 0,25x gravado migra sozinho.
     *
     * 0,25x saiu da tabela porque o `create()` do Kokoro tem `assert speed >= 0.5`. Não existe
     * código de migração: quem aproxima é o [ClaudeDockSettings.effectiveSpeechSpeed], que já
     * escolhia o valor mais próximo em vez de só limitar a faixa. Sem este teste, isso é
     * esperança, não garantia.
     */
    @Test
    fun `velocidade 25 gravada antes da v1_11 vira 50`() {
        val antigo = ClaudeDockSettings().apply { speechSpeed = 25 }
        val atual = ClaudeDockSettings()

        atual.loadState(antigo.state)

        assertEquals(50, atual.effectiveSpeechSpeed())
    }

    /** T-1.84 (RF-54): o motor nasce no Kokoro e sobrevive ao loadState. */
    @Test
    fun `motor de voz nasce no kokoro e persiste`() {
        assertEquals(TtsEngine.KOKORO, ClaudeDockSettings().ttsEngine)
        assertEquals(ClaudeDockSettings.DEFAULT_KOKORO_VOICE, ClaudeDockSettings().effectiveKokoroVoice())
        assertEquals(ClaudeDockSettings.DEFAULT_KOKORO_PYTHON, ClaudeDockSettings().effectiveKokoroPython())

        val source = ClaudeDockSettings().apply {
            ttsEngine = TtsEngine.PIPER
            kokoroVoice = "pm_alex"
        }
        val target = ClaudeDockSettings()

        target.loadState(source.state)

        assertEquals(TtsEngine.PIPER, target.ttsEngine)
        assertEquals("pm_alex", target.effectiveKokoroVoice())
    }

    /** Campo limpo na tela cai no padrão, em vez de desabilitar a síntese sem explicação. */
    @Test
    fun `campos do kokoro em branco caem no padrao`() {
        val settings = ClaudeDockSettings().apply {
            kokoroPython = "   "
            kokoroVoice = ""
        }

        assertEquals(ClaudeDockSettings.DEFAULT_KOKORO_PYTHON, settings.effectiveKokoroPython())
        assertEquals(ClaudeDockSettings.DEFAULT_KOKORO_VOICE, settings.effectiveKokoroVoice())
    }
}
