package dev.reginaldomorais.claudedock

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import java.nio.file.Files

/**
 * T-1.21: verificação do executável (CB-01, CB-02, CB-37).
 *
 * A verificação é **advisória** — quem resolve o nome de verdade é o shell da sessão, com o
 * `PATH` do `.zshrc`/`.bashrc`. Estes testes cobrem só o que ela consegue afirmar sozinha, e o
 * caso que gerou notificação indevida: binário em diretório que o IDE não tem no `PATH` (R-17).
 */
class ClaudeExecutableCheckTest : BasePlatformTestCase() {

    fun `test caminho explicito executavel e aceito`() {
        val file = tempFile("claude-dock-exec", executable = true)

        assertTrue(ClaudeTerminalSessionFactory.isExecutableAvailable(file.path))
    }

    fun `test caminho explicito sem permissao de execucao e recusado`() {
        val file = tempFile("claude-dock-noexec", executable = false)

        assertFalse(ClaudeTerminalSessionFactory.isExecutableAvailable(file.path))
    }

    fun `test caminho explicito inexistente e recusado`() {
        assertFalse(
            ClaudeTerminalSessionFactory.isExecutableAvailable("/tmp/claude-dock-ausente-xyz"),
        )
    }

    fun `test campo em branco e recusado`() {
        assertFalse(ClaudeTerminalSessionFactory.isExecutableAvailable("   "))
    }

    /**
     * O caso do R-17: o binário existe em `~/.local/bin`, que o IDE não tem no `PATH` porque
     * herda o ambiente da sessão gráfica, e não do `.zshrc`.
     */
    fun `test nome fora do PATH e aceito pelos diretorios conhecidos`() {
        val dir = tempDir()
        File(dir, PROBE_NAME).apply {
            createNewFile()
            deleteOnExit()
            setExecutable(true)
        }

        assertTrue(ClaudeTerminalSessionFactory.isExecutableAvailable(PROBE_NAME, listOf(dir.path)))
    }

    fun `test binario sem permissao no diretorio conhecido e recusado`() {
        val dir = tempDir()
        File(dir, PROBE_NAME).apply {
            createNewFile()
            deleteOnExit()
            setExecutable(false)
        }

        assertFalse(
            ClaudeTerminalSessionFactory.isExecutableAvailable(PROBE_NAME, listOf(dir.path)),
        )
    }

    fun `test diretorio conhecido sem o binario e recusado`() {
        assertFalse(
            ClaudeTerminalSessionFactory.isExecutableAvailable(PROBE_NAME, listOf(tempDir().path)),
        )
    }

    private fun tempFile(prefix: String, executable: Boolean): File =
        File.createTempFile(prefix, "").apply {
            deleteOnExit()
            setExecutable(executable)
        }

    private fun tempDir(): File =
        Files.createTempDirectory("claude-dock-bin").toFile().apply { deleteOnExit() }

    private companion object {
        /**
         * Nome inexistente em qualquer `PATH`, de propósito.
         *
         * Com o nome real (`claude`), os testes de fallback passariam pelo **motivo errado**:
         * o `PATH` de quem roda a suíte resolve antes, e o fallback nunca seria exercitado.
         */
        const val PROBE_NAME = "claude-dock-probe"
    }
}
