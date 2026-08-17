package dev.reginaldomorais.claudedock

import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.JPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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

    /**
     * T-1.72 — o subtítulo é escrito na faixa que já existe (RF-61).
     *
     * A asserção é sobre **pixels alterados na faixa de cima**, e não sobre a aparência: o que
     * precisa ser fixado é que algo foi desenhado ali, sem depender de fonte, tema ou escala.
     */
    @Test
    fun `subtitulo e escrito na faixa de cima`() {
        val component = JPanel()
        ClaudeSessionPadding.apply(component, terminalPanel(Color.BLACK), PADDING)

        assertEquals("sem nome, a faixa é só cor", 0, paintedPixelsInTopBand(component, Color.BLACK))

        ClaudeSessionPadding.setSubtitle(component, "backend")

        assertTrue(
            "esperava texto pintado na faixa de cima",
            paintedPixelsInTopBand(component, Color.BLACK) > 0,
        )
    }

    /**
     * T-1.72 / CB-69 — faixa curta demais não recebe texto.
     *
     * É o que transforma `MIN_PADDING = 0` em degradação silenciosa em vez de texto invadindo o
     * terminal. Forçar o respiro a crescer seria desfazer o layout que o usuário pediu (R-32).
     */
    @Test
    fun `faixa curta demais nao recebe subtitulo`() {
        val component = JPanel()
        ClaudeSessionPadding.apply(component, terminalPanel(Color.BLACK), 2)
        ClaudeSessionPadding.setSubtitle(component, "backend")

        assertEquals(0, paintedPixelsInTopBand(component, Color.BLACK))
    }

    /** RF-62: em branco apaga o nome, e a faixa volta a ser só cor. */
    @Test
    fun `subtitulo em branco e apagado`() {
        val component = JPanel()
        ClaudeSessionPadding.apply(component, terminalPanel(Color.BLACK), PADDING)

        ClaudeSessionPadding.setSubtitle(component, "backend")
        ClaudeSessionPadding.setSubtitle(component, "   ")

        assertNull(ClaudeSessionPadding.subtitleOf(component))
        assertEquals(0, paintedPixelsInTopBand(component, Color.BLACK))
    }

    /** Sem faixa nossa não há subtítulo — e não há exceção (engine fora do CLASSIC). */
    @Test
    fun `sem painel do terminal o subtitulo nao explode`() {
        val component = JPanel()
        ClaudeSessionPadding.apply(component, null, PADDING)

        ClaudeSessionPadding.setSubtitle(component, "backend")

        assertEquals("backend", ClaudeSessionPadding.subtitleOf(component))
    }

    /**
     * DEF-11 — o rótulo automático da divisão é distinto por pane.
     *
     * Nomes iguais devolveriam o problema que ele existe para resolver: com duas faixas dizendo a
     * mesma coisa, continua não havendo como saber qual pane o menu atinge.
     */
    @Test
    fun `rotulo automatico distingue as panes`() {
        assertEquals("Sessão 1", ClaudeSessionPadding.autoLabel(1))
        assertEquals("Sessão 2", ClaudeSessionPadding.autoLabel(2))
        assertNotEquals(ClaudeSessionPadding.autoLabel(1), ClaudeSessionPadding.autoLabel(2))
    }

    /** O rótulo automático é ponto de partida: renomear por cima é um rename comum (RF-62). */
    @Test
    fun `rotulo automatico e substituivel e apagavel`() {
        val component = JPanel()
        ClaudeSessionPadding.apply(component, terminalPanel(Color.BLACK), PADDING)

        ClaudeSessionPadding.setSubtitle(component, ClaudeSessionPadding.autoLabel(2))
        assertEquals("Sessão 2", ClaudeSessionPadding.subtitleOf(component))

        ClaudeSessionPadding.setSubtitle(component, "frontend")
        assertEquals("frontend", ClaudeSessionPadding.subtitleOf(component))

        ClaudeSessionPadding.setSubtitle(component, "")
        assertNull(ClaudeSessionPadding.subtitleOf(component))
    }

    private fun terminalPanel(color: Color): JComponent = JPanel().apply { background = color }

    /** Pixels da faixa de cima que diferem do fundo — a prova de que algo foi escrito nela. */
    private fun paintedPixelsInTopBand(component: JComponent, background: Color): Int {
        val image = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()

        try {
            component.border.paintBorder(component, graphics, 0, 0, SIZE, SIZE)
        } finally {
            graphics.dispose()
        }

        val top = component.border.getBorderInsets(component).top
        var painted = 0

        for (row in 0 until minOf(top, SIZE)) {
            for (column in 0 until SIZE) {
                if (image.getRGB(column, row) != background.rgb) painted++
            }
        }

        return painted
    }

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
