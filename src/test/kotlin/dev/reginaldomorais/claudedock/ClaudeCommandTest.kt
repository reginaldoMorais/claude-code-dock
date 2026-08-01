package dev.reginaldomorais.claudedock

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-1.5 e T-1.6: montagem do comando e proteção contra interpretação pelo shell. */
class ClaudeCommandTest {

    @Test
    fun `nova sessao executa o CLI`() {
        assertEquals("claude", ClaudeCommand.newSession("claude"))
    }

    @Test
    fun `retomada delega o historico ao CLI`() {
        assertEquals("claude --resume", ClaudeCommand.resumeSession("claude"))
    }

    @Test
    fun `token simples passa intacto`() {
        assertEquals("claude", ClaudeCommand.quote("claude"))
        assertEquals("/usr/local/bin/claude", ClaudeCommand.quote("/usr/local/bin/claude"))
    }

    @Test
    fun `caminho com espaco recebe aspas`() {
        assertEquals("'/opt/my tools/claude'", ClaudeCommand.quote("/opt/my tools/claude"))
    }

    @Test
    fun `caminho com espaco continua um unico argumento na retomada`() {
        assertEquals(
            "'/opt/my tools/claude' --resume",
            ClaudeCommand.resumeSession("/opt/my tools/claude"),
        )
    }

    @Test
    fun `metacaracteres do shell sao neutralizados`() {
        // Sem aspas, o shell executaria "rm -rf ." como comando separado.
        assertEquals("'claude; rm -rf .'", ClaudeCommand.quote("claude; rm -rf ."))
        assertEquals("'claude \$(whoami)'", ClaudeCommand.quote("claude \$(whoami)"))
        assertEquals("'claude && echo'", ClaudeCommand.quote("claude && echo"))
    }

    @Test
    fun `aspa simples no caminho e escapada corretamente`() {
        // Fecha, escapa a aspa literal e reabre — forma POSIX.
        assertEquals("""'it'\''s'""", ClaudeCommand.quote("it's"))
    }

    @Test
    fun `valor vazio vira string vazia citada`() {
        assertEquals("''", ClaudeCommand.quote(""))
    }

    @Test
    fun `saida plana acrescenta a flag antes dos demais argumentos`() {
        assertEquals("claude --ax-screen-reader", ClaudeCommand.newSession("claude", flatOutput = true))
        assertEquals(
            "claude --ax-screen-reader --resume",
            ClaudeCommand.resumeSession("claude", flatOutput = true),
        )
    }

    @Test
    fun `saida plana desligada e o padrao`() {
        assertEquals("claude", ClaudeCommand.newSession("claude"))
        assertEquals("claude --resume", ClaudeCommand.resumeSession("claude"))
    }

    @Test
    fun `saida plana convive com caminho citado`() {
        assertEquals(
            "'/opt/my tools/claude' --ax-screen-reader --resume",
            ClaudeCommand.resumeSession("/opt/my tools/claude", flatOutput = true),
        )
    }
}
