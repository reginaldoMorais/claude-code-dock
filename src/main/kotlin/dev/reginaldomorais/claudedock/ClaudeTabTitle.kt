package dev.reginaldomorais.claudedock

/**
 * Gera títulos distinguíveis entre abas (RF-16, CB-13).
 *
 * Lógica pura, separada do `ContentManager` para poder ser testada.
 */
object ClaudeTabTitle {

    const val BASE = "Claude"

    fun next(usedTitles: Set<String>): String {
        if (BASE !in usedTitles) return BASE

        var index = 2
        while ("$BASE ($index)" in usedTitles) index++
        return "$BASE ($index)"
    }
}
