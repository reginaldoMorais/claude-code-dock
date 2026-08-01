package dev.reginaldomorais.claudedock

/**
 * Monta as linhas de comando enviadas ao shell da sessão.
 *
 * Lógica pura e sem dependência da plataforma, para poder ser testada sem subir um IDE.
 */
object ClaudeCommand {

    /** Caracteres que fazem o shell interpretar o token de forma especial. */
    private val SAFE_TOKEN = Regex("^[A-Za-z0-9_@%+=:,./-]+$")

    /** Sessão nova: apenas executa o CLI. */
    fun newSession(executable: String): String = buildCommand(executable)

    /** Retomada: delega o histórico ao próprio CLI (D-02). */
    fun resumeSession(executable: String): String = buildCommand(executable, "--resume")

    private fun buildCommand(executable: String, vararg args: String): String =
        (listOf(quote(executable)) + args).joinToString(" ")

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
