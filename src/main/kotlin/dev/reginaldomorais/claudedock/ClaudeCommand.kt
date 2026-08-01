package dev.reginaldomorais.claudedock

/**
 * Monta as linhas de comando enviadas ao shell da sessão.
 *
 * Lógica pura e sem dependência da plataforma, para poder ser testada sem subir um IDE.
 */
object ClaudeCommand {

    /** Caracteres que fazem o shell interpretar o token de forma especial. */
    private val SAFE_TOKEN = Regex("^[A-Za-z0-9_@%+=:,./-]+$")

    /**
     * Saída plana do CLI: sem bordas decorativas nem animações.
     *
     * Pensada para leitores de tela, serve também a quem acha o TUI carregado dentro de uma
     * tool window estreita — é a alternativa barata a reimplementar a UI (Q-14).
     */
    const val FLAT_OUTPUT_FLAG = "--ax-screen-reader"

    /** Sessão nova: apenas executa o CLI. */
    fun newSession(executable: String, flatOutput: Boolean = false): String =
        buildCommand(executable, flatOutput)

    /** Retomada: delega o histórico ao próprio CLI (D-02). */
    fun resumeSession(executable: String, flatOutput: Boolean = false): String =
        buildCommand(executable, flatOutput, "--resume")

    private fun buildCommand(
        executable: String,
        flatOutput: Boolean,
        vararg args: String,
    ): String = buildList {
        add(quote(executable))
        if (flatOutput) add(FLAT_OUTPUT_FLAG)
        addAll(args)
    }.joinToString(" ")

    /**
     * Protege o caminho do executável contra interpretação pelo shell.
     *
     * O comando é digitado no shell da sessão (mesma abordagem do plugin oficial), então
     * caminhos com espaço ou caractere especial precisam de aspas. Tokens simples como
     * `claude` passam intactos para não poluir o que o usuário vê no terminal.
     */
    fun quote(value: String): String {
        if (value.isEmpty()) return "''"
        if (SAFE_TOKEN.matches(value)) return value
        // Aspas simples no estilo POSIX: encerra, escapa a aspa e reabre.
        return "'" + value.replace("'", "'\\''") + "'"
    }
}
