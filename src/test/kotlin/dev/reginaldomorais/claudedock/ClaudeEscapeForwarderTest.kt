package dev.reginaldomorais.claudedock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.event.KeyEvent

/** Cobre a regra que decide quando o Esc é devolvido ao shell. */
class ClaudeEscapeForwarderTest {

    @Test
    fun `encaminha Esc puro no key pressed`() {
        assertTrue(
            ClaudeEscapeForwarder.shouldForward(
                id = KeyEvent.KEY_PRESSED,
                keyCode = KeyEvent.VK_ESCAPE,
                modifiersEx = 0,
                consumed = false,
            ),
        )
    }

    @Test
    fun `encaminha Ctrl+Backspace como Esc`() {
        assertTrue(
            ClaudeEscapeForwarder.shouldForward(
                id = KeyEvent.KEY_PRESSED,
                keyCode = KeyEvent.VK_BACK_SPACE,
                modifiersEx = KeyEvent.CTRL_DOWN_MASK,
                consumed = false,
            ),
        )
    }

    @Test
    fun `ignora Backspace puro para deixar apagar texto no terminal`() {
        assertFalse(
            ClaudeEscapeForwarder.shouldForward(KeyEvent.KEY_PRESSED, KeyEvent.VK_BACK_SPACE, 0, false),
        )
    }

    @Test
    fun `ignora key released e key typed para nao duplicar o envio`() {
        assertFalse(
            ClaudeEscapeForwarder.shouldForward(KeyEvent.KEY_RELEASED, KeyEvent.VK_ESCAPE, 0, false),
        )
        assertFalse(
            ClaudeEscapeForwarder.shouldForward(KeyEvent.KEY_TYPED, KeyEvent.VK_ESCAPE, 0, false),
        )
    }

    @Test
    fun `ignora outras teclas`() {
        assertFalse(
            ClaudeEscapeForwarder.shouldForward(KeyEvent.KEY_PRESSED, KeyEvent.VK_ENTER, 0, false),
        )
    }

    @Test
    fun `ignora Esc com modificador para preservar atalhos do IDE`() {
        assertFalse(
            ClaudeEscapeForwarder.shouldForward(
                KeyEvent.KEY_PRESSED,
                KeyEvent.VK_ESCAPE,
                KeyEvent.SHIFT_DOWN_MASK,
                false,
            ),
        )
        assertFalse(
            ClaudeEscapeForwarder.shouldForward(
                KeyEvent.KEY_PRESSED,
                KeyEvent.VK_ESCAPE,
                KeyEvent.CTRL_DOWN_MASK,
                false,
            ),
        )
    }

    @Test
    fun `ignora evento ja consumido por outro handler`() {
        assertFalse(
            ClaudeEscapeForwarder.shouldForward(KeyEvent.KEY_PRESSED, KeyEvent.VK_ESCAPE, 0, true),
        )
    }
}
