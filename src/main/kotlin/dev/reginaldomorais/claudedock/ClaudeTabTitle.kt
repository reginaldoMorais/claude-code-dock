package dev.reginaldomorais.claudedock

/**
 * Gera títulos distinguíveis entre abas (RF-16, CB-13) e compõe o rótulo visível (RF-59).
 *
 * Lógica pura, separada do `ContentManager` para poder ser testada.
 */
object ClaudeTabTitle {

    const val BASE = "Claude"

    /** Marca a aba que ficou sem nenhuma sessão viva (RF-11, RF-44). */
    const val ENDED_SUFFIX = "encerrado"

    private const val ENDED_MARK = " ($ENDED_SUFFIX)"

    fun next(usedTitles: Set<String>): String {
        if (BASE !in usedTitles) return BASE

        var index = 2
        while ("$BASE ($index)" in usedTitles) index++
        return "$BASE ($index)"
    }

    /**
     * Rótulo visível da aba: o nome mais o estado, quando há (RF-59).
     *
     * **É o único lugar que compõe as duas coisas.** Até a v1.11 a interpolação vivia dentro de um
     * `invokeLater` em [ClaudeDockSessions], onde nenhum teste unitário alcançava — e o rótulo da
     * aba nunca foi o nome dela, e sim nome + estado. Um rename que escrevesse `displayName` direto
     * apagaria o sufixo numa ordem e o nome na outra, passando em qualquer teste feliz (Achado 43).
     *
     * **Idempotente de propósito:** aplicar sobre um rótulo que já traz o sufixo não o duplica.
     * Sem isso, renomear uma aba encerrada devolveria `"(encerrado) (encerrado)"` — exatamente o
     * que a chave `TAB_TITLE` existe para evitar, agora entrando pela porta da frente.
     */
    fun display(base: String, ended: Boolean): String {
        val clean = base.removeSuffix(ENDED_MARK)

        return if (ended) "$clean$ENDED_MARK" else clean
    }
}
