package dev.reginaldomorais.claudedock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission

/** T-1.13 e T-1.14: obtenção da conversa pelo `/export` (RF-24, RF-25, CB-29). */
class ClaudeSessionExportTest {

    @Test
    fun `o caminho vai cru no comando, sem aspas`() {
        val target = Path.of("/tmp/claude dock/export.md")

        // Aspas POSIX virariam parte do nome: o CLI usa o argumento como veio, só com trim.
        assertEquals("/export /tmp/claude dock/export.md\r", ClaudeSessionExport.command(target))
    }

    @Test
    fun `o comando usa caminho absoluto`() {
        val command = ClaudeSessionExport.command(Path.of("export.md"))

        assertTrue(command, command.startsWith("/export /"))
        assertTrue(command, command.endsWith("export.md\r"))
    }

    @Test
    fun `o destino termina em md para o CLI nao acrescentar txt`() {
        val target = ClaudeSessionExport.createTarget()

        try {
            assertTrue(target.toString(), target.toString().endsWith(".md"))
        } finally {
            ClaudeSessionExport.delete(target)
        }
    }

    @Test
    fun `o destino nasce acessivel so pelo dono`() {
        val target = ClaudeSessionExport.createTarget()

        try {
            val view = Files.getFileAttributeView(target, PosixFileAttributeView::class.java)
                ?: return // Sistema sem POSIX: nada a verificar.

            val permissions = view.readAttributes().permissions()
            val forOthers = permissions - setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            )

            assertTrue("Permissões: $permissions", forOthers.isEmpty())
        } finally {
            ClaudeSessionExport.delete(target)
        }
    }

    @Test
    fun `le o conteudo assim que o arquivo deixa de estar vazio`() {
        val target = ClaudeSessionExport.createTarget()

        try {
            Files.writeString(target, "❯ pergunta   \n\n● resposta\n\n\n")

            assertEquals("❯ pergunta\n\n● resposta", ClaudeSessionExport.awaitContent(target))
        } finally {
            ClaudeSessionExport.delete(target)
        }
    }

    @Test
    fun `arquivo vazio dentro do prazo devolve nulo`() {
        val target = ClaudeSessionExport.createTarget()

        try {
            assertNull(ClaudeSessionExport.awaitContent(target, timeoutMs = 60, pollIntervalMs = 10))
        } finally {
            ClaudeSessionExport.delete(target)
        }
    }

    @Test
    fun `arquivo inexistente devolve nulo sem lancar`() {
        val missing = Path.of("/tmp/claude-dock-inexistente-${System.nanoTime()}.md")

        assertNull(ClaudeSessionExport.awaitContent(missing, timeoutMs = 60, pollIntervalMs = 10))
    }

    @Test
    fun `delete remove o arquivo e tolera ausencia`() {
        val target = ClaudeSessionExport.createTarget()
        Files.writeString(target, "conversa")

        ClaudeSessionExport.delete(target)
        assertFalse(Files.exists(target))

        // Segunda chamada não pode explodir: o `finally` roda mesmo em caminho de erro.
        ClaudeSessionExport.delete(target)
    }
}
