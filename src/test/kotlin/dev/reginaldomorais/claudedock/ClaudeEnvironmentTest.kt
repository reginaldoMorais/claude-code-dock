package dev.reginaldomorais.claudedock

import com.intellij.util.SystemProperties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-1.8: tradução do CLAUDE_CONFIG_DIR configurado em variáveis de ambiente. */
class ClaudeEnvironmentTest {

    @Test
    fun `sem configuracao nao injeta nada`() {
        assertTrue(ClaudeEnvironment.build(null).isEmpty())
        assertTrue(ClaudeEnvironment.build("").isEmpty())
        assertTrue(ClaudeEnvironment.build("   ").isEmpty())
    }

    @Test
    fun `caminho absoluto vira CLAUDE_CONFIG_DIR`() {
        assertEquals(
            mapOf("CLAUDE_CONFIG_DIR" to "/opt/claude-work"),
            ClaudeEnvironment.build("/opt/claude-work"),
        )
    }

    @Test
    fun `espacos ao redor sao removidos`() {
        assertEquals(
            mapOf("CLAUDE_CONFIG_DIR" to "/opt/claude-work"),
            ClaudeEnvironment.build("  /opt/claude-work  "),
        )
    }

    @Test
    fun `til e expandido porque variavel de ambiente nao passa pelo shell`() {
        val home = SystemProperties.getUserHome()
        assertEquals(
            mapOf("CLAUDE_CONFIG_DIR" to "$home/.claude-work"),
            ClaudeEnvironment.build("~/.claude-work"),
        )
    }
}
