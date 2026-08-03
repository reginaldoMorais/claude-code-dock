package dev.reginaldomorais.claudedock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-1.40: limite de navegação entre abas (DEF-02). */
class ClaudeTabNavigationTest {

    @Test
    fun `com uma aba so nao ha para onde navegar`() {
        // O caso que quebrou: `selectPreviousContent` faz assertTrue(count > 1) antes de tudo.
        assertFalse(ClaudeTabNavigation.canNavigate(1))
    }

    @Test
    fun `sem aba nenhuma tambem nao`() {
        assertFalse(ClaudeTabNavigation.canNavigate(0))
    }

    @Test
    fun `com duas ou mais abas a navegacao e permitida`() {
        assertTrue(ClaudeTabNavigation.canNavigate(2))
        assertTrue(ClaudeTabNavigation.canNavigate(7))
    }
}
