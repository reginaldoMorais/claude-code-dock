package dev.reginaldomorais.claudedock

import com.intellij.openapi.diagnostic.Logger
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import java.awt.event.KeyEvent

/**
 * Encaminha Esc puro e Ctrl+Backspace como Esc ao shell.
 *
 * Em GoLand 2026.2, o Esc é bloqueado globalmente. Ctrl+Backspace é a alternativa
 * para sair de comandos interativos como `/usage`. Backspace puro segue o
 * comportamento padrão (apagar texto no terminal).
 */
object ClaudeEscapeForwarder {

    /** Sequência ESC do VT100, o mesmo byte que o JediTerm enviaria. */
    private const val ESC = ""

    private val LOG = Logger.getInstance(ClaudeEscapeForwarder::class.java)

    /** Instala o interceptador no painel JediTerm por trás de [widget]. */
    fun install(widget: TerminalWidget) {
        val panel = JBTerminalWidget.asJediTermWidget(widget)?.terminalPanel ?: return

        panel.addPreKeyEventHandler { event ->
            if (!shouldForward(event.id, event.keyCode, event.modifiersEx, event.isConsumed)) {
                return@addPreKeyEventHandler
            }

            // Sem PTY ainda (sessão adiada até a UI aparecer): deixa o comportamento padrão.
            val connector = widget.ttyConnector ?: return@addPreKeyEventHandler

            try {
                connector.write(ESC)
                event.consume()
            } catch (e: Exception) {
                // Falha na escrita não pode derrubar a EDT; o Esc segue para o listener padrão.
                LOG.warn("Falha ao encaminhar Esc para o shell", e)
            }
        }
    }

    /**
     * Regra pura de encaminhamento, isolada para poder ser testada sem UI.
     *
     * Encaminha Esc puro e Ctrl+Backspace como Esc ao shell.
     * Backspace puro segue o comportamento padrão (apagar texto).
     */
    fun shouldForward(id: Int, keyCode: Int, modifiersEx: Int, consumed: Boolean): Boolean =
        id == KeyEvent.KEY_PRESSED &&
            !consumed &&
            (
                (keyCode == KeyEvent.VK_ESCAPE && modifiersEx == 0) ||
                (keyCode == KeyEvent.VK_BACK_SPACE && modifiersEx == KeyEvent.CTRL_DOWN_MASK)
            )
}
