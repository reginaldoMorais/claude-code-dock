package dev.reginaldomorais.claudedock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** T-1.12: normalização do buffer copiado (RF-21). */
class ClaudeSessionTextTest {

    @Test
    fun `buffer ausente ou vazio nao produz texto`() {
        assertNull(ClaudeSessionText.normalize(null))
        assertNull(ClaudeSessionText.normalize(""))
    }

    @Test
    fun `buffer so com espacos e quebras nao produz texto`() {
        assertNull(ClaudeSessionText.normalize("   \n\n \t \n"))
    }

    @Test
    fun `remove espacos a direita de cada linha`() {
        assertEquals(
            "> pergunta\n⏺ resposta",
            ClaudeSessionText.normalize("> pergunta    \n⏺ resposta      "),
        )
    }

    @Test
    fun `remove as linhas vazias da tela abaixo do prompt`() {
        assertEquals(
            "⏺ resposta",
            ClaudeSessionText.normalize("⏺ resposta\n     \n\n          \n"),
        )
    }

    @Test
    fun `preserva indentacao e linhas em branco internas`() {
        val raw = "primeira\n\n    indentada   \nultima"

        assertEquals("primeira\n\n    indentada\nultima", ClaudeSessionText.normalize(raw))
    }
}
