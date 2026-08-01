package dev.reginaldomorais.claudedock

import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Graphics
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.border.Border

/**
 * Afasta o conteúdo da sessão das bordas da tool window.
 *
 * O componente do widget é um `JPanel` com `BorderLayout`, então uma borda vazia diminui a área
 * entregue ao terminal e o grid de caracteres se recalcula sozinho — `TerminalPanel` mede a si
 * mesmo, não a janela. É por isso que a borda vai no widget e não no `Content` da aba.
 *
 * O JediTerm reserva 4px à esquerda (`getInsetX`, constante e `protected`) e nada nos demais
 * lados: daí o conteúdo colado nas bordas.
 */
object ClaudeSessionPadding {

    /**
     * [padding] vem em pixels lógicos das configurações; o `JBUI` escala para monitores HiDPI.
     *
     * [terminalPanel] é o painel do JediTerm, usado só como fonte de cor. Vem nulo quando não há
     * painel por trás (engine diferente do CLASSIC): aí a faixa fica transparente e quem a pinta
     * é a tool window.
     */
    fun apply(component: JComponent, terminalPanel: JComponent?, padding: Int) {
        component.isOpaque = false
        component.border =
            if (terminalPanel == null) JBUI.Borders.empty(padding)
            else TerminalBackgroundBorder(terminalPanel, padding)
    }

    /**
     * Faixa pintada com o fundo do terminal, lido **a cada pintura**.
     *
     * `TerminalPanel.getBackground()` delega a `getWindowBackground()` e é recalculado a cada
     * chamada: copiar a cor uma vez deixaria a faixa na cor antiga quando o usuário trocasse de
     * tema com a sessão aberta. Lendo no `paintBorder`, a faixa acompanha sem listener nenhum.
     */
    private class TerminalBackgroundBorder(
        private val terminalPanel: JComponent,
        private val padding: Int,
    ) : Border {

        override fun getBorderInsets(c: Component): Insets = JBUI.insets(padding)

        override fun isBorderOpaque(): Boolean = true

        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
            val insets = getBorderInsets(c)
            g.color = terminalPanel.background

            g.fillRect(x, y, width, insets.top)
            g.fillRect(x, y + height - insets.bottom, width, insets.bottom)
            g.fillRect(x, y + insets.top, insets.left, height - insets.top - insets.bottom)
            g.fillRect(
                x + width - insets.right,
                y + insets.top,
                insets.right,
                height - insets.top - insets.bottom,
            )
        }
    }
}
