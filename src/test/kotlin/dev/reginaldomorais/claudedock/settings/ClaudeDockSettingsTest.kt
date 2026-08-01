package dev.reginaldomorais.claudedock.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `saida plana vem desligada e persiste ligada`() {
        assertFalse(ClaudeDockSettings().flatOutput)

        val source = ClaudeDockSettings().apply { flatOutput = true }
        val target = ClaudeDockSettings()

        target.loadState(source.state)

        assertTrue(target.flatOutput)
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
}
