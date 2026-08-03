package dev.reginaldomorais.claudedock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

/** T-1.28 a T-1.31: gravação do trecho selecionado (RF-33, RF-34, CB-43, CB-46). */
class ClaudeSelectionExportTest {

    @Test
    fun `o nome sugerido carrega prefixo, carimbo de tempo e extensao`() {
        val name = ClaudeSelectionExport.suggestedFileName(
            LocalDateTime.of(2026, 8, 3, 14, 5, 9),
        )

        assertEquals("claude-selection-20260803-140509.md", name)
    }

    @Test
    fun `dois instantes diferentes produzem nomes diferentes`() {
        val first = ClaudeSelectionExport.suggestedFileName(LocalDateTime.of(2026, 8, 3, 14, 5, 9))
        val second = ClaudeSelectionExport.suggestedFileName(LocalDateTime.of(2026, 8, 3, 14, 5, 10))

        // Sem isto, duas exportações seguidas sugeririam o mesmo destino (RF-34).
        assertNotEquals(first, second)
    }

    @Test
    fun `a extensao oferecida ao dialogo nao leva ponto`() {
        // O FileSaverDescriptor espera "md", e não ".md".
        assertEquals("md", ClaudeSelectionExport.EXTENSION)
        assertTrue(ClaudeSelectionExport.suggestedFileName().endsWith(".md"))
    }

    @Test
    fun `write grava em utf-8 e o round-trip devolve o mesmo texto`() {
        val target = Files.createTempFile("claude-selection-test-", ".md")
        val text = "❯ pergunta com acentuação\n\n● resposta — com travessão\n\tindentado"

        try {
            ClaudeSelectionExport.write(target, text)

            assertEquals(text, Files.readString(target, StandardCharsets.UTF_8))
        } finally {
            Files.deleteIfExists(target)
        }
    }

    @Test
    fun `write sobrescreve conteudo anterior sem deixar sobra`() {
        val target = Files.createTempFile("claude-selection-test-", ".md")

        try {
            ClaudeSelectionExport.write(target, "texto longo que sera substituido")
            ClaudeSelectionExport.write(target, "curto")

            assertEquals("curto", Files.readString(target))
        } finally {
            Files.deleteIfExists(target)
        }
    }

    @Test(expected = java.io.IOException::class)
    fun `write em diretorio inexistente lanca em vez de falhar em silencio`() {
        // É esta exceção que o chamador converte em notificação (CB-46, Fluxo J).
        val impossible = Path.of("/tmp/claude-dock-inexistente-${System.nanoTime()}/trecho.md")

        ClaudeSelectionExport.write(impossible, "conteúdo")
    }

    @Test
    fun `selecao so de espacos normaliza para nulo e nao chega a abrir o dialogo`() {
        // Reuso verificado: a guarda de CB-43 é ClaudeSessionText, não código novo.
        assertNull(ClaudeSessionText.normalize("   \n  \n\t\n"))
        assertNull(ClaudeSessionText.normalize(""))

        // A indentação à esquerda **sobrevive** — é o que faz o trecho exportado continuar
        // válido quando o que se selecionou foi código.
        assertEquals("    texto", ClaudeSessionText.normalize("    texto   \n\n"))
    }
}
