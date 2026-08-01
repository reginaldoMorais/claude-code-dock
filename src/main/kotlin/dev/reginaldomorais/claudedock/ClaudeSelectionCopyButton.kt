package dev.reginaldomorais.claudedock

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jediterm.terminal.model.TerminalSelection
import com.jediterm.terminal.model.TerminalSelectionChangesListener
import com.jediterm.terminal.ui.TerminalPanel
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/**
 * Botão flutuante que copia apenas o trecho selecionado (RF-26).
 *
 * `Ctrl+C` e `Ctrl+Shift+C` já copiam a seleção, mas são invisíveis para quem usa o mouse —
 * o botão é descoberta, não capacidade nova. Por isso ele falha em silêncio: sem o painel
 * JediTerm por trás (engine que não seja o CLASSIC), nada é instalado e os atalhos continuam
 * valendo (R-15).
 *
 * O botão aparece ao **soltar** o botão do mouse, e não a cada mudança de seleção: durante o
 * arraste a seleção muda a cada pixel, e um popup piscando junto seria inutilizável.
 */
object ClaudeSelectionCopyButton {

    fun install(widget: TerminalWidget, parent: Disposable) {
        val jediTermWidget = JBTerminalWidget.asJediTermWidget(widget) ?: return
        val panel = jediTermWidget.terminalPanel

        val controller = Controller(jediTermWidget, panel)
        panel.addMouseListener(controller)
        panel.addSelectionListener(controller)

        Disposer.register(parent) {
            panel.removeMouseListener(controller)
            panel.removeSelectionListener(controller)
            controller.hide()
        }
    }

    private class Controller(
        private val widget: JBTerminalWidget,
        private val panel: TerminalPanel,
    ) : MouseAdapter(), TerminalSelectionChangesListener {

        private var popup: JBPopup? = null

        override fun mouseReleased(e: MouseEvent) {
            if (!SwingUtilities.isLeftMouseButton(e)) return

            // A seleção só está consolidada depois que o painel termina de tratar o evento.
            val location = Point(e.x, e.y)
            SwingUtilities.invokeLater {
                if (selectedText().isNullOrBlank()) hide() else show(location)
            }
        }

        /** Some quando a seleção é desfeita, inclusive por teclado ou por clique simples. */
        override fun selectionChanged(selection: TerminalSelection?) {
            if (selection == null) SwingUtilities.invokeLater { hide() }
        }

        private fun show(at: Point) {
            hide()

            if (!panel.isShowing) return

            val label = JBLabel(AllIcons.Actions.Copy).apply {
                border = JBUI.Borders.empty(4)
                toolTipText = "Copiar seleção"
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mousePressed(e: MouseEvent) = copySelection()
                })
            }

            popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(label, null)
                // Sem roubar o foco: o usuário continua digitando na sessão.
                .setRequestFocus(false)
                .setResizable(false)
                .setMovable(false)
                .setCancelOnClickOutside(true)
                .createPopup()
                // Deslocado do ponteiro para não nascer debaixo dele.
                .also { it.show(RelativePoint(panel, Point(at.x + OFFSET, at.y + OFFSET))) }
        }

        private fun copySelection() {
            selectedText()?.takeIf { it.isNotBlank() }?.let(CopyPasteManager::copyTextToClipboard)
            hide()
        }

        fun hide() {
            popup?.takeIf { !it.isDisposed }?.cancel()
            popup = null
        }

        private fun selectedText(): String? = widget.selectedText
    }

    private const val OFFSET = 8
}
