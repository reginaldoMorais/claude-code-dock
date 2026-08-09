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
import com.intellij.openapi.util.Disposer
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jediterm.terminal.ui.TerminalPanel
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Popup flutuante sobre o trecho selecionado: copiar (RF-26), exportar para arquivo (RF-33) e
 * tocar (RF-50).
 *
 * `Ctrl+C` e `Ctrl+Shift+C` já copiam a seleção, mas são invisíveis para quem usa o mouse —
 * a cópia é descoberta, não capacidade nova. Por isso o popup falha em silêncio: sem o painel
 * JediTerm por trás (engine que não seja o CLASSIC), nada é instalado e os atalhos continuam
 * valendo (R-15, CB-47).
 *
 * **O critério de entrada mudou na v1.10, e o antigo está registrado porque errou.** Até aqui a
 * pergunta era "isso já existe em outro lugar da UI?", e foi ela que recusou o play duas vezes
 * (RF-30 em v1.5.1, e de novo no Achado 26). Essa pergunta reprova também o botão de copiar, que
 * existe logo acima apesar do `Ctrl+C` — ela media a existência da capacidade e ignorava o custo
 * de alcançá-la. O critério em vigor (Achado 34): entra o que opera **sobre o trecho selecionado**
 * e cabe em **um clique**. Teto de três botões (R-29, que substitui R-23).
 *
 * O popup aparece ao **soltar** o botão do mouse, e não a cada mudança de seleção: durante o
 * arraste a seleção muda a cada pixel, e um popup piscando junto seria inutilizável.
 *
 * **DEF-09 — por que a barra não é mais um `JBPopup`.** Ela era, e morria sozinha ~270 ms depois
 * de nascer. A pilha do fechamento, capturada com um `JBPopupListener` temporário, nomeou o
 * culpado: `IdePopupManager.closeAllPopups`, a partir de um evento de foco. **A plataforma fecha
 * todos os popups registrados**, e o nosso ia junto — sem que ninguém tivesse clicado nele.
 *
 * O que o log também descartou, com números: largura (painel de 80px, em `x=325`, num terminal de
 * 2173px — sobra espaço) e, com ela, a contagem de botões. E o `scrollArea` limpando seleção, que
 * é mecanismo real e verificado, **não era a causa deste sintoma** (Achado 36).
 *
 * A saída é não ser popup. Um filho do `JLayeredPane` em `POPUP_LAYER` aparece por cima do
 * terminal do mesmo jeito, e o `closeAllPopups` **não o enxerga** — não há registro na plataforma
 * para fechar. De quebra somem, juntos, o cancelamento por clique fora, por desativação de janela
 * e as restrições de `xdg_popup` do Wayland, onde este IDE roda (`sun.awt.wl.WLToolkit`).
 *
 * Funciona **com ou sem `Shift`**, que é o requisito. Uma tentativa anterior exigiu `Shift` — o
 * gesto sem ele pertence ao TUI, que liga mouse reporting (`?1000h`/`?1006h`) — e foi **recusada
 * pelo usuário**: a barra tem de aparecer nos dois casos.
 *
 * Duas escolhas menores sobreviveram ao diagnóstico errado porque se sustentam sozinhas: não se
 * ouve mais `TerminalSelectionChangesListener` (o `scrollArea` limpa a seleção a cada rolagem), e
 * os botões agem sobre o trecho capturado em [snapshot], não sobre o que restou na tela.
 */
object ClaudeSelectionCopyButton {

    fun install(widget: TerminalWidget, parent: Disposable, project: Project) {
        val jediTermWidget = JBTerminalWidget.asJediTermWidget(widget) ?: return
        val panel = jediTermWidget.terminalPanel

        val controller = Controller(jediTermWidget, panel, project)
        panel.addMouseListener(controller)

        Disposer.register(parent) {
            panel.removeMouseListener(controller)
            controller.hide()
        }
    }

    private class Controller(
        private val widget: JBTerminalWidget,
        private val panel: TerminalPanel,
        private val project: Project,
    ) : MouseAdapter() {

        private var overlay: JComponent? = null

        /**
         * O trecho como estava quando o popup nasceu (DEF-09).
         *
         * **Os botões não podem reler `widget.selectedText` na hora do clique.** Entre mostrar e
         * clicar, qualquer saída do CLI pode ter apagado a seleção — e aí copiar, exportar ou
         * tocar operariam sobre `null`, em silêncio. O que o usuário selecionou é o que o popup
         * carrega.
         */
        private var snapshot: String? = null

        override fun mouseReleased(e: MouseEvent) {
            if (!SwingUtilities.isLeftMouseButton(e)) return

            // A seleção só está consolidada depois que o painel termina de tratar o evento.
            val location = Point(e.x, e.y)
            SwingUtilities.invokeLater {
                val text = selectedText()
                if (text.isNullOrBlank()) hide() else show(location, text)
            }
        }

        private fun show(at: Point, text: String) {
            hide()

            if (!panel.isShowing) return
            val layers = SwingUtilities.getRootPane(panel)?.layeredPane ?: return

            snapshot = text

            val buttons = buttonPanel(::copySelection, ::exportSelection, ::playSelection)
            // Opaco e com borda: sem a moldura do JBPopup, é isto que o separa do terminal.
            buttons.isOpaque = true
            buttons.background = UIUtil.getPanelBackground()
            buttons.border = JBUI.Borders.customLine(JBColor.border())

            // Deslocado do ponteiro para não nascer debaixo dele.
            val origin = SwingUtilities.convertPoint(
                panel,
                Point(at.x + OFFSET, at.y + OFFSET),
                layers,
            )
            buttons.bounds = Rectangle(origin, buttons.preferredSize)

            // POPUP_LAYER fica acima do conteúdo, e um filho do layered pane **não** é um popup
            // da plataforma — é exatamente por isso que ele sobrevive (DEF-09).
            layers.add(buttons, JLayeredPane.POPUP_LAYER, 0)
            buttons.bounds.let { layers.repaint(it.x, it.y, it.width, it.height) }

            overlay = buttons
        }

        private fun copySelection() {
            snapshot?.takeIf { it.isNotBlank() }?.let(CopyPasteManager::copyTextToClipboard)
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
            val text = ClaudeSessionText.normalize(snapshot)
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

        /**
         * Toca o trecho selecionado (RF-50).
         *
         * Chama o **mesmo** [ClaudeTtaSessions.playText] do menu "Áudio" (RNF-32): a velocidade
         * de RF-47, o pausar/retomar de RF-31 e a exclusão mútua entre falas de RNF-23 continuam
         * com um dono só. Um segundo caminho de síntese seria um segundo estado de áudio.
         *
         * Piper ausente não é tratado aqui — o aviso vive dentro do `playText`, que é por onde
         * este botão e o item do menu passam (D-42).
         */
        private fun playSelection() {
            val text = ClaudeSessionText.normalize(snapshot)
            hide()

            if (text == null) {
                return notify("Não há nada para tocar na seleção.", NotificationType.WARNING)
            }

            ClaudeTtaSessions.getInstance(project).playText(text)
        }

        private fun notify(message: String, type: NotificationType) =
            ClaudeDockSessions.getInstance(project).notify(message, type)

        fun hide() {
            overlay?.let { current ->
                val area = current.bounds
                current.parent?.let { parent ->
                    parent.remove(current)
                    parent.repaint(area.x, area.y, area.width, area.height)
                }
            }
            overlay = null
            snapshot = null
        }

        private fun selectedText(): String? = widget.selectedText
    }

    /**
     * Monta a barra do popup (RF-50).
     *
     * Fora do `Controller` porque o teto de botões é regra de produto — R-29, teto de três — e
     * precisa de guarda de regressão que rode sem terminal, sem PTY e sem IDE (T-1.64). Dentro do
     * `Controller` só se testaria subindo um `TerminalPanel` de verdade.
     */
    internal fun buttonPanel(
        onCopy: () -> Unit,
        onExport: () -> Unit,
        onPlay: () -> Unit,
    ): JPanel = JPanel(FlowLayout(FlowLayout.CENTER, 2, 0)).apply {
        isOpaque = false
        add(button(AllIcons.Actions.Copy, COPY_TOOLTIP, onCopy))
        add(button(AllIcons.ToolbarDecorator.Export, EXPORT_TOOLTIP, onExport))
        add(button(AllIcons.Actions.Execute, PLAY_TOOLTIP, onPlay))
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

    internal const val COPY_TOOLTIP = "Copiar seleção"
    internal const val EXPORT_TOOLTIP = "Exportar seleção para arquivo"
    internal const val PLAY_TOOLTIP = "Tocar seleção"

    private const val OFFSET = 8

    private val LOG = Logger.getInstance(ClaudeSelectionCopyButton::class.java)
}
