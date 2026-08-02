package dev.reginaldomorais.claudedock

import com.intellij.openapi.diagnostic.Logger
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import java.awt.event.KeyEvent

/**
 * Encaminha Backspace como Esc ao shell.
 *
 * Em GoLand 2026.2, o Esc é bloqueado globalmente. Como alternativa, o Backspace
 * é encaminhado como Esc para sair de comandos interativos como `/usage`.
 * Em IntelliJ, o Esc puro funciona via pre-handler.
 */
object ClaudeEscapeForwarder {

    /** Sequência ESC do VT100, o mesmo byte que o JediTerm enviaria. */
    private const val ESC = "\u001b"

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
     * Encaminha Esc (e Backspace em GoLand) como Esc ao shell.
     * Combinações com modificador continuam com o tratamento original do IDE.
     */
    fun shouldForward(id: Int, keyCode: Int, modifiersEx: Int, consumed: Boolean): Boolean =
        id == KeyEvent.KEY_PRESSED &&
            (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_BACK_SPACE) &&
            modifiersEx == 0 &&
            !consumed
}
