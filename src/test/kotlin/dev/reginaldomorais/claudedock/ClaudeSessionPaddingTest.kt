package dev.reginaldomorais.claudedock

import java.awt.Color
import javax.swing.JPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-1.18: respiro entre a sessão e as bordas da tool window. */
class ClaudeSessionPaddingTest {

    @Test
    fun `aplica borda vazia nos quatro lados`() {
        val component = JPanel()

        ClaudeSessionPadding.apply(component, null, PADDING)

        val insets = component.border.getBorderInsets(component)
        assertTrue("esperava respiro em todos os lados", insets.top > 0)
        assertEquals(insets.top, insets.bottom)
        assertEquals(insets.left, insets.right)
        assertEquals(insets.top, insets.left)
    }

    @Test
    fun `pinta o respiro com o fundo do terminal`() {
        val component = JPanel()

        ClaudeSessionPadding.apply(component, Color.RED, PADDING)

        assertEquals(Color.RED, component.background)
        assertTrue("sem opaco o fundo não é pintado", component.isOpaque)
    }

    @Test
    fun `sem painel JediTerm aplica so a borda`() {
        val component = JPanel().apply { background = Color.BLUE }

        ClaudeSessionPadding.apply(component, null, PADDING)

        assertTrue(component.border.getBorderInsets(component).top > 0)
        assertEquals(Color.BLUE, component.background)
    }

    @Test
    fun `respiro maior produz borda maior`() {
        val estreito = JPanel().also { ClaudeSessionPadding.apply(it, null, 8) }
        val largo = JPanel().also { ClaudeSessionPadding.apply(it, null, 24) }

        assertTrue(
            "o valor configurado precisa chegar na borda",
            largo.border.getBorderInsets(largo).top > estreito.border.getBorderInsets(estreito).top,
        )
    }

    /** Zero é válido: volta ao comportamento cru do JediTerm. */
    @Test
    fun `respiro zero nao adiciona borda`() {
        val component = JPanel()

        ClaudeSessionPadding.apply(component, null, 0)

        assertEquals(0, component.border.getBorderInsets(component).top)
    }

    private companion object {
        const val PADDING = 20
    }
}
