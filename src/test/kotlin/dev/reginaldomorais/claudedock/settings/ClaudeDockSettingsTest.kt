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
        assertEquals(25, ClaudeDockSettings().apply { speechSpeed = 0 }.effectiveSpeechSpeed())
        assertEquals(100, ClaudeDockSettings().apply { speechSpeed = 110 }.effectiveSpeechSpeed())
        assertEquals(125, ClaudeDockSettings().apply { speechSpeed = 130 }.effectiveSpeechSpeed())
    }

    /** T-1.54: a tabela é a fonte única do menu e da tela — as duas UIs não podem divergir. */
    @Test
    fun `tabela de velocidades cobre de 0,25x a 2x`() {
        assertEquals(
            listOf(25, 50, 75, 100, 125, 150, 175, 200),
            ClaudeDockSettings.SPEECH_SPEEDS.keys.toList(),
        )
        assertEquals("0,25x", ClaudeDockSettings.speechSpeedLabel(25))
        assertEquals("1,75x", ClaudeDockSettings.speechSpeedLabel(175))
        assertEquals("110%", ClaudeDockSettings.speechSpeedLabel(110))
    }
}
