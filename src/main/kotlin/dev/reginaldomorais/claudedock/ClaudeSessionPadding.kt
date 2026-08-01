package dev.reginaldomorais.claudedock

import com.intellij.util.ui.JBUI
import java.awt.Color
import javax.swing.JComponent

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
     * [background] é a cor do painel do terminal. O `JPanel` do widget não define fundo próprio,
     * então sem ela o respiro sairia na cor de painel do tema, e não na do terminal. Vem nulo
     * quando não há painel JediTerm por trás (engine diferente do CLASSIC) — nesse caso aplica-se
     * só a borda, e quem pinta o fundo é o componente do próprio engine.
     */
    fun apply(component: JComponent, background: Color?, padding: Int) {
        component.border = JBUI.Borders.empty(padding)

        if (background != null) {
            // ponytail: cor lida uma vez. Trocar de tema com a sessão aberta deixa o respiro na
            // cor antiga até a aba ser recriada; ouvir LafManagerListener só se incomodar.
            component.isOpaque = true
            component.background = background
        }
    }
}
