package dev.reginaldomorais.claudedock

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Grava em arquivo apenas o trecho selecionado na sessão (RF-33).
 *
 * **Não passa pelo `/export` do CLI, e isso não é escolha de simplicidade** (D-30): o `/export`
 * roda dentro do Claude Code e exporta *a conversa* — o único argumento que ele aceita é o
 * caminho do destino. Não há como pedir a ele um trecho da tela.
 *
 * O trecho, por outro lado, já está na mão do plugin desde RF-26: é o mesmo texto que alimenta
 * o botão de copiar. Exportá-lo é gravar algo que já temos, sem PTY, sem arquivo temporário e
 * sem espera — por isso é a única saída do plugin que funciona com a sessão ocupada ou
 * encerrada.
 *
 * Objeto puro, sem dependência de UI, para poder ser testado sem subir o IDE (RNF-26).
 */
object ClaudeSelectionExport {

    private const val PREFIX = "claude-selection-"

    /** Mesmo sufixo de RF-24: o conteúdo é do mesmo tipo. */
    private const val SUFFIX = ".md"

    private val STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    /** Extensão oferecida ao diálogo nativo, que cuida do sufixo e da sobrescrita (CB-45). */
    const val EXTENSION = "md"

    /**
     * Nome sugerido no diálogo de salvar.
     *
     * O carimbo de tempo distingue exportações sucessivas sem perguntar nada ao usuário: duas
     * seguidas não se sobrescrevem (RF-34).
     */
    fun suggestedFileName(now: LocalDateTime = LocalDateTime.now()): String =
        "$PREFIX${STAMP.format(now)}$SUFFIX"

    /**
     * Grava o texto em UTF-8.
     *
     * A `IOException` sobe de propósito: quem sabe notificar o usuário é o chamador (RNF-25), e
     * este objeto não conhece `Project` nem `Notification`.
     */
    fun write(target: Path, text: String) {
        Files.writeString(target, text, StandardCharsets.UTF_8)
    }
}
