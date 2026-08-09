package dev.reginaldomorais.claudedock

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.event.MouseEvent
import javax.swing.JComponent

/**
 * T-1.64 — a barra do popup da seleção tem três botões, nesta ordem (RF-50, R-29).
 *
 * **É guarda de regra de produto, não de Swing.** O teto de botões já foi rompido uma vez por
 * critério errado (Achado 34) e vale registrar o número em código: quem quiser um quarto botão
 * quebra este teste e vai ler o porquê antes.
 */
class ClaudeSelectionCopyButtonTest : BasePlatformTestCase() {

    fun `test T-1_64 o popup monta tres botoes na ordem copiar exportar tocar`() {
        val panel = ClaudeSelectionCopyButton.buttonPanel({}, {}, {})

        assertEquals("Teto de três botões (R-29)", 3, panel.componentCount)
        assertEquals(
            listOf(
                ClaudeSelectionCopyButton.COPY_TOOLTIP,
                ClaudeSelectionCopyButton.EXPORT_TOOLTIP,
                ClaudeSelectionCopyButton.PLAY_TOOLTIP,
            ),
            panel.components.map { (it as JComponent).toolTipText },
        )
    }

    /** Cada botão dispara **o seu** callback — trocar dois na fiação é o erro fácil aqui. */
    fun `test T-1_64 cada botao chama o proprio callback`() {
        val chamados = mutableListOf<String>()
        val panel = ClaudeSelectionCopyButton.buttonPanel(
            { chamados += "copiar" },
            { chamados += "exportar" },
            { chamados += "tocar" },
        )

        panel.components.forEach { component ->
            val click = MouseEvent(
                component,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                0,
                0,
                0,
                1,
                false,
            )
            component.mouseListeners.forEach { it.mousePressed(click) }
        }

        assertEquals(listOf("copiar", "exportar", "tocar"), chamados)
    }
}
