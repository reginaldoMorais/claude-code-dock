package dev.reginaldomorais.claudedock.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** T-1.9: valor padrão, persistência e caminho efetivo do CLAUDE_CONFIG_DIR por projeto. */
class ClaudeDockProjectSettingsTest {

    @Test
    fun `valor padrao e vazio para deixar o CLI decidir`() {
        assertEquals("", ClaudeDockProjectSettings().claudeConfigDir)
        assertNull(ClaudeDockProjectSettings().effectiveConfigDir())
    }

    @Test
    fun `estado sobrevive a copia via loadState`() {
        val source = ClaudeDockProjectSettings().apply { claudeConfigDir = "/opt/claude-work" }
        val target = ClaudeDockProjectSettings()

        target.loadState(source.state)

        assertEquals("/opt/claude-work", target.claudeConfigDir)
    }

    @Test
    fun `campo em branco nao injeta a variavel`() {
        val settings = ClaudeDockProjectSettings().apply { claudeConfigDir = "   " }
        assertNull(settings.effectiveConfigDir())
    }

    @Test
    fun `espacos ao redor sao removidos`() {
        val settings = ClaudeDockProjectSettings().apply { claudeConfigDir = "  /opt/claude-work  " }
        assertEquals("/opt/claude-work", settings.effectiveConfigDir())
    }
}
