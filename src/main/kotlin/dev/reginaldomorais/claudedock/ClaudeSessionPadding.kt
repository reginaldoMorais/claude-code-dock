package dev.reginaldomorais.claudedock

import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Graphics
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.border.Border

/**
 * Afasta o conteúdo da sessão das bordas da tool window, e escreve o nome da sessão nessa faixa.
 *
 * O componente do widget é um `JPanel` com `BorderLayout`, então uma borda vazia diminui a área
 * entregue ao terminal e o grid de caracteres se recalcula sozinho — `TerminalPanel` mede a si
 * mesmo, não a janela. É por isso que a borda vai no widget e não no `Content` da aba.
 *
 * O JediTerm reserva 4px à esquerda (`getInsetX`, constante e `protected`) e nada nos demais
 * lados: daí o conteúdo colado nas bordas.
 *
 * _(v1.12)_ A faixa de cima passa a ser também onde mora o **subtítulo da pane** (RF-61). Ela já
 * era espaço reservado e repintado a cada frame, então o nome não rouba um pixel de altura do
 * terminal — que é exatamente a objeção pela qual Q-28 recusou uma barra de título por pane.
 */
object ClaudeSessionPadding {

    /**
     * Subtítulo da pane, pendurado no próprio componente que a borda decora (RNF-37).
     *
     * Vive como `clientProperty` pelo mesmo motivo de D-18 e D-36: morre junto com o componente,
     * sem mapa a limpar. E sobrevive de graça a `swap`, `rotate` e `close` (RF-43, RF-39), porque
     * o splitter reparenta componentes em vez de copiar estado (CB-74).
     */
    private const val PANE_SUBTITLE = "ClaudeDockPaneSubtitle"

    /**
     * [padding] vem em pixels lógicos das configurações; o `JBUI` escala para monitores HiDPI.
     *
     * [terminalPanel] é o painel do JediTerm, usado só como fonte de cor. Vem nulo quando não há
     * painel por trás (engine diferente do CLASSIC): aí a faixa fica transparente e quem a pinta
     * é a tool window — e, sem faixa nossa, também não há subtítulo. Na prática não acontece: a
     * sessão é sempre CLASSIC (Achado 27).
     */
    fun apply(component: JComponent, terminalPanel: JComponent?, padding: Int) {
        component.isOpaque = false
        component.border =
            if (terminalPanel == null) JBUI.Borders.empty(padding)
            else TerminalBackgroundBorder(terminalPanel, padding)
    }

    /**
     * Rótulo automático da pane quando a aba se divide (RF-60, DEF-11).
     *
     * Existe porque a v1.12 nasceu com o subtítulo **vazio até o usuário nomear**, e o uso real
     * mostrou o que isso significa: nada aparece, ninguém descobre que dá para nomear, e — pior —
     * não há como saber **qual** pane o item do menu vai atingir, já que ele age sobre a que está
     * em foco. É o Achado 29 outra vez: entregue e invisível é quase o mesmo que não entregue.
     */
    fun autoLabel(index: Int): String = "Sessão $index"

    /** Nome dado pelo usuário a esta sessão, ou `null` quando nunca foi nomeada (RF-60). */
    fun subtitleOf(component: JComponent): String? =
        component.getClientProperty(PANE_SUBTITLE) as? String

    /**
     * Define o subtítulo da pane e repinta a faixa.
     *
     * Em branco **apaga**, e a faixa volta a ser só cor — em branco significa "volte ao padrão",
     * não erro (RF-62).
     */
    fun setSubtitle(component: JComponent, subtitle: String?) {
        component.putClientProperty(PANE_SUBTITLE, subtitle?.trim()?.ifEmpty { null })
        component.repaint()
    }

    /**
     * Faixa pintada com o fundo do terminal, lido **a cada pintura**.
     *
     * `TerminalPanel.getBackground()` delega a `getWindowBackground()` e é recalculado a cada
     * chamada: copiar a cor uma vez deixaria a faixa na cor antiga quando o usuário trocasse de
     * tema com a sessão aberta. Lendo no `paintBorder`, a faixa acompanha sem listener nenhum —
     * e o mesmo vale para a cor do subtítulo (CB-75).
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

            drawSubtitle(c, g, x, y, width, insets)
        }

        /**
         * Escreve o subtítulo na faixa de cima, quando há um e ele cabe (RF-61).
         *
         * Nada é pintado quando a faixa é mais curta que a linha de texto — é o que transforma
         * `MIN_PADDING = 0` em degradação silenciosa em vez de texto invadindo o terminal
         * (CB-69, R-32). Forçar o respiro a crescer seria desfazer o layout que o usuário pediu.
         */
        private fun drawSubtitle(
            c: Component,
            g: Graphics,
            x: Int,
            y: Int,
            width: Int,
            insets: Insets,
        ) {
            val subtitle = (c as? JComponent)?.getClientProperty(PANE_SUBTITLE) as? String
            if (subtitle.isNullOrEmpty()) return

            val font = UIUtil.getLabelFont(UIUtil.FontSize.SMALL)
            val metrics = c.getFontMetrics(font)
            val area = width - insets.left - insets.right
            if (metrics.height > insets.top || area <= 0) return

            // `Graphics` derivado: fonte, cor e recorte não vazam para quem pintar depois. O custo
            // é uma derivação por frame, e só quando existe subtítulo (RNF-38).
            val scratch = g.create()

            try {
                scratch.font = font
                scratch.color = UIUtil.getContextHelpForeground()
                // Nome longo demais é recortado na borda da pane, sem invadir a faixa da direita
                // nem o terminal — o mesmo tratamento que a plataforma dá ao rótulo da aba (CB-70).
                scratch.clipRect(x + insets.left, y, area, insets.top)
                scratch.drawString(
                    subtitle,
                    x + insets.left,
                    y + (insets.top + metrics.ascent - metrics.descent) / 2,
                )
            } finally {
                scratch.dispose()
            }
        }
    }
}
