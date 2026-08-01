package dev.reginaldomorais.claudedock

import com.intellij.openapi.diagnostic.Logger
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import java.awt.event.KeyEvent

/**
 * Devolve a tecla Esc ao shell quando a sessão roda fora da tool window "Terminal".
 *
 * `com.intellij.terminal.TerminalEscapeKeyListener` só entrega o Esc ao processo quando
 * o terminal está na tool window de id "Terminal"; em qualquer outra ele consome a tecla
 * e move o foco para o editor. Como o Claude Code usa Esc para sair de comandos como
 * `/usage`, interceptamos o evento antes desse listener, escrevemos o Esc no PTY e
 * consumimos o evento — o resultado é o mesmo comportamento do terminal do IDE.
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
     * Só o Esc puro é redirecionado: combinações com modificador continuam com o
     * tratamento original do IDE.
     */
    fun shouldForward(id: Int, keyCode: Int, modifiersEx: Int, consumed: Boolean): Boolean =
        id == KeyEvent.KEY_PRESSED &&
            keyCode == KeyEvent.VK_ESCAPE &&
            modifiersEx == 0 &&
            !consumed
}
