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
}
