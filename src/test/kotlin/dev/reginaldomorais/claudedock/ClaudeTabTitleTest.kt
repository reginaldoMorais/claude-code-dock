package dev.reginaldomorais.claudedock

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-1.7: títulos de aba distinguíveis. */
class ClaudeTabTitleTest {

    @Test
    fun `primeira aba usa o titulo base`() {
        assertEquals("Claude", ClaudeTabTitle.next(emptySet()))
    }

    @Test
    fun `abas seguintes recebem sufixo incremental`() {
        assertEquals("Claude (2)", ClaudeTabTitle.next(setOf("Claude")))
        assertEquals("Claude (3)", ClaudeTabTitle.next(setOf("Claude", "Claude (2)")))
    }

    @Test
    fun `reaproveita lacuna deixada por aba fechada`() {
        assertEquals("Claude (2)", ClaudeTabTitle.next(setOf("Claude", "Claude (3)")))
    }

    @Test
    fun `ignora titulos alheios`() {
        assertEquals("Claude", ClaudeTabTitle.next(setOf("Terminal", "Local")))
    }

    /** T-1.67: o rótulo é nome + estado, e este é o único ponto que compõe os dois (RF-59). */
    @Test
    fun `display compoe o nome com o estado`() {
        assertEquals("backend", ClaudeTabTitle.display("backend", ended = false))
        assertEquals("backend (encerrado)", ClaudeTabTitle.display("backend", ended = true))
    }

    /**
     * T-1.68: aplicar sobre um rótulo que já traz o sufixo não o duplica.
     *
     * É o "(encerrado) (encerrado)" que a chave `TAB_TITLE` sempre existiu para evitar — e que um
     * rename ingênuo faria entrar pela porta da frente, renomeando uma aba já encerrada.
     */
    @Test
    fun `display nao acumula o sufixo`() {
        assertEquals(
            "backend (encerrado)",
            ClaudeTabTitle.display("backend (encerrado)", ended = true),
        )
    }

    /** Tirar o estado devolve o nome limpo — o caminho de quem lê o `displayName` de fora. */
    @Test
    fun `display sem estado remove o sufixo herdado`() {
        assertEquals("backend", ClaudeTabTitle.display("backend (encerrado)", ended = false))
    }

    /** O sufixo só sai quando é sufixo de verdade: nome que apenas contém a palavra fica intacto. */
    @Test
    fun `display nao mutila nome que contem a palavra`() {
        assertEquals("encerrado do dia", ClaudeTabTitle.display("encerrado do dia", ended = false))
    }
}
