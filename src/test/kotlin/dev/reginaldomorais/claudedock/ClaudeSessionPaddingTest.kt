package dev.reginaldomorais.claudedock

import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.JPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-1.18: respiro entre a sessão e as bordas da tool window. */
class ClaudeSessionPaddingTest {

    @Test
    fun `aplica borda vazia nos quatro lados`() {
        val component = JPanel()

        ClaudeSessionPadding.apply(component, terminalPanel(Color.BLACK), PADDING)

        val insets = component.border.getBorderInsets(component)
        assertTrue("esperava respiro em todos os lados", insets.top > 0)
        assertEquals(insets.top, insets.bottom)
        assertEquals(insets.left, insets.right)
        assertEquals(insets.top, insets.left)
    }

    @Test
    fun `faixa usa o fundo do terminal`() {
        val panel = terminalPanel(Color.RED)
        val component = JPanel()

        ClaudeSessionPadding.apply(component, panel, PADDING)

        assertEquals(Color.RED, cornerColor(component))
    }

    /**
     * O ponto da borda customizada: a cor é lida na pintura, então trocar o tema com a sessão
     * aberta muda a faixa junto. Copiar a cor uma vez deixava a faixa velha.
     */
    @Test
    fun `faixa acompanha a troca de cor do terminal`() {
        val panel = terminalPanel(Color.RED)
        val component = JPanel()

        ClaudeSessionPadding.apply(component, panel, PADDING)
        assertEquals(Color.RED, cornerColor(component))

        panel.background = Color.WHITE

        assertEquals(Color.WHITE, cornerColor(component))
    }

    /** Sem painel JediTerm a faixa fica transparente: quem pinta é a tool window. */
    @Test
    fun `sem painel do terminal a borda fica vazia`() {
        val component = JPanel().apply { isOpaque = true }

        ClaudeSessionPadding.apply(component, null, PADDING)

        assertFalse(component.isOpaque)
        assertTrue(component.border.getBorderInsets(component).top > 0)
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

    private fun terminalPanel(color: Color): JComponent = JPanel().apply { background = color }

    /** Pinta a borda num bitmap e devolve a cor do canto superior esquerdo — dentro da faixa. */
    private fun cornerColor(component: JComponent): Color {
        val image = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()

        try {
            component.border.paintBorder(component, graphics, 0, 0, SIZE, SIZE)
        } finally {
            graphics.dispose()
        }

        return Color(image.getRGB(0, 0))
    }

    private companion object {
        const val PADDING = 20
        const val SIZE = 100
    }
}
