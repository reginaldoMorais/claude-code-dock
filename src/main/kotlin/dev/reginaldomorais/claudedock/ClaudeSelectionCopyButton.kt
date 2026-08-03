package dev.reginaldomorais.claudedock

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
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
import java.awt.FlowLayout
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Popup flutuante sobre o trecho selecionado: copiar (RF-26) e exportar para arquivo (RF-33).
 *
 * `Ctrl+C` e `Ctrl+Shift+C` já copiam a seleção, mas são invisíveis para quem usa o mouse —
 * a cópia é descoberta, não capacidade nova. Por isso o popup falha em silêncio: sem o painel
 * JediTerm por trás (engine que não seja o CLASSIC), nada é instalado e os atalhos continuam
 * valendo (R-15, CB-47).
 *
 * A exportação, ao contrário, **é** capacidade nova: o botão do cabeçalho exporta a conversa
 * inteira pelo `/export` do CLI, e não há como pedir a ele um trecho (D-30). É esse o critério
 * que autoriza o segundo botão e que recusou um terceiro (o play de RF-30, descartado em
 * v1.5.1): capacidade que não existe em outro lugar, não simetria com o cabeçalho. Teto
 * declarado de dois botões (R-23).
 *
 * O popup aparece ao **soltar** o botão do mouse, e não a cada mudança de seleção: durante o
 * arraste a seleção muda a cada pixel, e um popup piscando junto seria inutilizável.
 */
object ClaudeSelectionCopyButton {

    fun install(widget: TerminalWidget, parent: Disposable, project: Project) {
        val jediTermWidget = JBTerminalWidget.asJediTermWidget(widget) ?: return
        val panel = jediTermWidget.terminalPanel

        val controller = Controller(jediTermWidget, panel, project)
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
        private val project: Project,
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

            val buttons = JPanel(FlowLayout(FlowLayout.CENTER, 2, 0)).apply {
                isOpaque = false
                add(button(AllIcons.Actions.Copy, "Copiar seleção", ::copySelection))
                add(
                    button(
                        AllIcons.ToolbarDecorator.Export,
                        "Exportar seleção para arquivo",
                        ::exportSelection,
                    ),
                )
            }

            popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(buttons, null)
                // Sem roubar o foco: o usuário continua digitando na sessão.
                .setRequestFocus(false)
                .setResizable(false)
                .setMovable(false)
                .setCancelOnClickOutside(true)
                .createPopup()
                // Deslocado do ponteiro para não nascer debaixo dele.
                .also { it.show(RelativePoint(panel, Point(at.x + OFFSET, at.y + OFFSET))) }
        }

        private fun button(icon: Icon, tooltip: String, onClick: () -> Unit) =
            JBLabel(icon).apply {
                border = JBUI.Borders.empty(4)
                toolTipText = tooltip
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mousePressed(e: MouseEvent) = onClick()
                })
            }

        private fun copySelection() {
            selectedText()?.takeIf { it.isNotBlank() }?.let(CopyPasteManager::copyTextToClipboard)
            hide()
        }

        /**
         * Grava o trecho selecionado no arquivo que o usuário escolher (RF-33).
         *
         * O diálogo é o nativo da plataforma: dele vêm de graça o filtro por extensão e a
         * confirmação de sobrescrita (CB-45). Cancelar devolve `null` — caminho normal, sem
         * nada a gravar nem a avisar (CB-44).
         */
        private fun exportSelection() {
            val text = ClaudeSessionText.normalize(selectedText())
            hide()

            if (text == null) {
                return notify("Não há nada para exportar na seleção.", NotificationType.WARNING)
            }

            val descriptor = FileSaverDescriptor(
                "Exportar Seleção",
                "Grava em arquivo apenas o trecho selecionado",
                ClaudeSelectionExport.EXTENSION,
            )
            val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
            val name = ClaudeSelectionExport.suggestedFileName()

            // Projeto sem basePath cai no diretório padrão da plataforma (CB-48).
            val baseDir = project.basePath?.let { Path.of(it) }
            val target = (if (baseDir != null) dialog.save(baseDir, name) else dialog.save(name))
                ?.file
                ?: return

            // Disco nunca na EDT (RNF-25).
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    ClaudeSelectionExport.write(target.toPath(), text)
                } catch (e: IOException) {
                    // O conteúdo do trecho nunca vai para o log (RNF-24).
                    LOG.warn("Falha ao gravar o trecho selecionado", e)

                    ApplicationManager.getApplication().invokeLater {
                        if (!project.isDisposed) {
                            notify(
                                "Não foi possível gravar o arquivo: ${e.message}",
                                NotificationType.ERROR,
                            )
                        }
                    }
                }
            }
        }

        private fun notify(message: String, type: NotificationType) =
            ClaudeDockSessions.getInstance(project).notify(message, type)

        fun hide() {
            popup?.takeIf { !it.isDisposed }?.cancel()
            popup = null
        }

        private fun selectedText(): String? = widget.selectedText
    }

    private const val OFFSET = 8

    private val LOG = Logger.getInstance(ClaudeSelectionCopyButton::class.java)
}
