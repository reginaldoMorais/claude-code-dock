package dev.reginaldomorais.claudedock

/**
 * Regra de navegação entre abas (DEF-02).
 *
 * `ContentManagerImpl.selectNextContent` e `selectPreviousContent` **começam** com
 * `LOG.assertTrue(getContentCount() > 1)` — verificado por `javap -c`. Ou seja, pedir navegação
 * com uma aba só não é operação inócua: é uma assertion no log do IDE, com stack trace inteiro.
 *
 * O menu de contexto do terminal mostra "Select Previous/Next Tab" sempre, sem consultar o
 * listener, então quem precisa segurar a chamada somos nós.
 *
 * Objeto puro para o limite ficar testável sem subir o IDE (RNF-26).
 */
object ClaudeTabNavigation {

    /** Só há para onde navegar com mais de uma aba aberta. */
    fun canNavigate(tabCount: Int): Boolean = tabCount > 1
}
