package dev.reginaldomorais.claudedock

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * T-1.57 a T-1.60 — a referência `@arquivo#Lx-y` enviada à sessão (RF-49).
 *
 * O formato foi lido do CLI, não inventado: estes testes são o que trava as três regras dele
 * (relativo ao cwd, 1-based, espaço no fim) contra alguém "arrumar" uma delas.
 */
class ClaudeEditorReferenceTest : BasePlatformTestCase() {

    /** T-1.57: uma linha só usa a forma curta — é o que o CLI faz quando `lineStart == lineEnd`. */
    fun `test uma linha usa a forma curta`() {
        assertEquals("@src/Main.kt#L3 ", ClaudeEditorReference.format("src/Main.kt", 3, 3))
    }

    /** T-1.58: várias linhas usam a faixa. */
    fun `test varias linhas usam a faixa`() {
        assertEquals("@src/Main.kt#L3-10 ", ClaudeEditorReference.format("src/Main.kt", 3, 10))
    }

    /**
     * T-1.59: sem seleção não vai `#L`.
     *
     * No CLI a condição é `if (e.lineStart && e.lineEnd)`, e em JS `0` é falso — por isso zero
     * significa "sem linhas", e não "linha zero".
     */
    fun `test sem selecao a referencia e so o arquivo`() {
        assertEquals("@src/Main.kt ", ClaudeEditorReference.format("src/Main.kt", 0, 0))
        assertEquals("@src/Main.kt ", ClaudeEditorReference.format("src/Main.kt", 0, 10))
    }

    /**
     * O espaço final não é enfeite: é ele que separa a menção do que o usuário digita depois.
     * Este teste existe porque `trim()` é a "limpeza" mais tentadora do mundo.
     */
    fun `test a referencia sempre termina em espaco`() {
        listOf(
            ClaudeEditorReference.format("a.kt", 1, 1),
            ClaudeEditorReference.format("a.kt", 1, 2),
            ClaudeEditorReference.format("a.kt", 0, 0),
        ).forEach { assertTrue("Faltou o espaço final em \"$it\"", it.endsWith(" ")) }
    }

    /** T-1.60: seleção de linhas inteiras não pode contar a linha seguinte. */
    fun `test selecao de linhas inteiras nao conta a linha seguinte`() {
        // Selecionou só a linha 3, e o offset final caiu na coluna 0 da linha 4.
        assertEquals(3, ClaudeEditorReference.inclusiveEndLine(3, 4, endsAtLineStart = true))
        // Selecionou da 3 à 10, terminando no início da 11.
        assertEquals(10, ClaudeEditorReference.inclusiveEndLine(3, 11, endsAtLineStart = true))
        // Terminou no meio da linha: nada a corrigir.
        assertEquals(4, ClaudeEditorReference.inclusiveEndLine(3, 4, endsAtLineStart = false))
        // Uma linha só, sem nada para descontar — não pode virar 2.
        assertEquals(3, ClaudeEditorReference.inclusiveEndLine(3, 3, endsAtLineStart = true))
    }

    fun `test caminho fora do projeto vai absoluto`() {
        assertEquals("src/Main.kt", ClaudeEditorReference.relativize("/proj", "/proj/src/Main.kt"))
        // Um "../../etc/passwd" não ajudaria o CLI em nada; o absoluto ele resolve.
        assertEquals("/outro/x.kt", ClaudeEditorReference.relativize("/proj", "/outro/x.kt"))
    }
}
