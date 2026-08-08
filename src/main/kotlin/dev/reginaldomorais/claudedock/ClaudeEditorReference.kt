package dev.reginaldomorais.claudedock

import java.nio.file.Path

/**
 * Monta a referência `@arquivo#Lx-y` que o Claude Code entende como menção (RF-49).
 *
 * **O formato não foi inventado aqui — foi lido do CLI 2.1.220**, na função que renderiza a
 * menção a partir da notificação MCP:
 *
 * ```js
 * let r = path.relative(cwd(), e.filePath)
 * if (e.lineStart && e.lineEnd)
 *   n = e.lineStart === e.lineEnd ? `@${r}#L${e.lineStart} ` : `@${r}#L${e.lineStart}-${e.lineEnd} `
 * else n = `@${r} `
 * ```
 *
 * Daí as três regras que [format] reproduz, e que não são óbvias de fora:
 * o caminho é **relativo ao cwd** da sessão, as linhas são **1-based** (o `&&` do original trata
 * `0` como ausente), e há **um espaço no fim** — é ele que separa a menção do que o usuário
 * digitar em seguida.
 *
 * Objeto puro, sem editor nem UI, para ser testável sem subir o IDE (mesmo critério de
 * [ClaudeSelectionExport], RNF-26).
 */
object ClaudeEditorReference {

    /**
     * @param startLine primeira linha da seleção, **1-based**. `0` ou menos significa "sem seleção".
     * @param endLine última linha da seleção, **1-based**, inclusiva.
     */
    fun format(relativePath: String, startLine: Int, endLine: Int): String = when {
        startLine <= 0 || endLine <= 0 -> "@$relativePath "
        startLine == endLine -> "@$relativePath#L$startLine "
        else -> "@$relativePath#L$startLine-$endLine "
    }

    /**
     * Caminho de [filePath] relativo ao diretório da sessão.
     *
     * Fora da árvore do projeto o CLI receberia um `../../..` inútil, então devolve o absoluto —
     * que ele resolve igual. Mesmo tratamento para caminhos que não se deixam relativizar
     * (drives distintos no Windows fazem `relativize` lançar).
     */
    fun relativize(basePath: String?, filePath: String): String {
        val base = ClaudeWorkingDirectory.resolve(basePath)

        return try {
            val relative = Path.of(base).relativize(Path.of(filePath)).toString()
            if (relative.isEmpty() || relative.startsWith("..")) filePath else relative
        } catch (e: IllegalArgumentException) {
            filePath
        }
    }

    /**
     * Última linha **inclusiva** de uma seleção que vai de [startLine] a [endLine].
     *
     * Selecionar linhas inteiras (com `Home`/`Shift+Down`, ou clicando na sarjeta) deixa o
     * offset final na **coluna 0 da linha seguinte**, que não faz parte da seleção. Sem esta
     * correção, selecionar uma linha reportaria duas — e o `@arquivo#L3-4` mentiria sobre o que
     * o usuário marcou.
     */
    fun inclusiveEndLine(startLine: Int, endLine: Int, endsAtLineStart: Boolean): Int =
        if (endsAtLineStart && endLine > startLine) endLine - 1 else endLine
}
