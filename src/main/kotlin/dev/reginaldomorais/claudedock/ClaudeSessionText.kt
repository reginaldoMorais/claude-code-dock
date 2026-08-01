package dev.reginaldomorais.claudedock

/**
 * Prepara o buffer do terminal para a área de transferência (RF-21).
 *
 * O texto que a plataforma devolve é o render literal da janela: o TUI do Claude Code
 * preenche a largura toda, e a tela tem dezenas de linhas vazias abaixo do prompt. As duas
 * coisas viram ruído ao colar.
 *
 * Objeto puro, sem dependência de UI, para poder ser testado sem subir o IDE.
 */
object ClaudeSessionText {

    /** Devolve `null` quando não há nada que valha copiar. */
    fun normalize(raw: CharSequence?): String? {
        if (raw == null) return null

        val text = raw.lineSequence()
            .joinToString("\n") { it.trimEnd() }
            .trimEnd()

        return text.ifBlank { null }
    }
}
