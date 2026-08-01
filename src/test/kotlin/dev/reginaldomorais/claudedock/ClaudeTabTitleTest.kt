package dev.reginaldomorais.claudedock

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-1.7: títulos de aba distinguíveis. */
class ClaudeTabTitleTest {

    @Test
    fun `primeira aba usa o titulo base`() {
        assertEquals("Claude", ClaudeTabTitle.next(emptySet()))
    }

    @Test
    fun `abas seguintes recebem sufixo incremental`() {
        assertEquals("Claude (2)", ClaudeTabTitle.next(setOf("Claude")))
        assertEquals("Claude (3)", ClaudeTabTitle.next(setOf("Claude", "Claude (2)")))
    }

    @Test
    fun `reaproveita lacuna deixada por aba fechada`() {
        assertEquals("Claude (2)", ClaudeTabTitle.next(setOf("Claude", "Claude (3)")))
    }

    @Test
    fun `ignora titulos alheios`() {
        assertEquals("Claude", ClaudeTabTitle.next(setOf("Terminal", "Local")))
    }
}
