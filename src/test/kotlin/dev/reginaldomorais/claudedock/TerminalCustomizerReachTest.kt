package dev.reginaldomorais.claudedock

import com.intellij.openapi.project.Project
import com.intellij.platform.eel.EelDescriptor
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jediterm.core.util.TermSize
import org.jetbrains.plugins.terminal.LocalTerminalCustomizer
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellStartupOptions

/**
 * T-4 — validação da premissa central do projeto (Q-01 / R-02).
 *
 * O plugin oficial da Anthropic injeta `CLAUDE_CODE_SSE_PORT` através de um
 * `LocalTerminalCustomizer`. Toda a arquitetura de casca fina depende de esse
 * extension point também alcançar as sessões criadas por
 * [ClaudeTerminalSessionFactory], fora da tool window nativa do terminal.
 *
 * Este teste registra um customizer próprio e verifica se a variável de fato chega
 * ao ambiente que o runner entrega ao processo.
 *
 * Se este teste falhar, a integração com o plugin oficial NÃO funciona na janela
 * dedicada e o SPEC precisa ser revisto antes de qualquer outra coisa.
 */
class TerminalCustomizerReachTest : BasePlatformTestCase() {

    fun `test customizer alcanca sessoes criadas fora da tool window nativa`() {
        ExtensionTestUtil.maskExtensions(
            LocalTerminalCustomizer.EP_NAME,
            listOf(SentinelCustomizer()),
            testRootDisposable,
        )

        val runner = LocalTerminalDirectRunner.createTerminalRunner(project)
        val options = ShellStartupOptions.Builder()
            .workingDirectory(ClaudeWorkingDirectory.resolve(project.basePath))
            .initialTermSize(TermSize(80, 24))
            .build()

        // Mesmo pré-processamento que `startShellTerminalWidget` executa internamente.
        val configured = runner.configureStartupOptions(options)

        assertEquals(
            "O LocalTerminalCustomizer NÃO alcançou a sessão: a arquitetura de casca fina " +
                "não se sustenta e o SPEC precisa ser revisto.",
            SENTINEL_VALUE,
            configured.envVariables[SENTINEL_VAR],
        )
    }

    fun `test sem customizer registrado a variavel nao aparece`() {
        ExtensionTestUtil.maskExtensions(
            LocalTerminalCustomizer.EP_NAME,
            emptyList(),
            testRootDisposable,
        )

        val runner = LocalTerminalDirectRunner.createTerminalRunner(project)
        val options = ShellStartupOptions.Builder()
            .workingDirectory(ClaudeWorkingDirectory.resolve(project.basePath))
            .initialTermSize(TermSize(80, 24))
            .build()

        // Controle: garante que o teste acima mede o customizer, e não um resíduo do ambiente.
        assertNull(runner.configureStartupOptions(options).envVariables[SENTINEL_VAR])
    }

    /** Reproduz o que o `TerminalCustomizer` do plugin oficial faz. */
    @Suppress("DEPRECATION")
    private class SentinelCustomizer : LocalTerminalCustomizer() {
        override fun customizeCommandAndEnvironment(
            project: Project,
            workingDirectory: String?,
            command: MutableList<String>,
            envs: MutableMap<String, String>,
            eelDescriptor: EelDescriptor,
        ): MutableList<String> {
            envs[SENTINEL_VAR] = SENTINEL_VALUE
            return command
        }
    }

    companion object {
        private const val SENTINEL_VAR = "CLAUDE_DOCK_REACH_PROBE"
        private const val SENTINEL_VALUE = "reached"
    }
}
