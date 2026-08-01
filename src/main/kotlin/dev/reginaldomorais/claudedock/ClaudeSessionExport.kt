package dev.reginaldomorais.claudedock

import com.intellij.openapi.diagnostic.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Obtém a conversa pelo `/export` do CLI, em vez de ler o buffer do terminal (RF-24).
 *
 * O buffer contém a conversa repetida — o TUI é Ink e repinta o frame inteiro a cada
 * redimensionamento da janela, empilhando uma cópia por repintura (DEF-01). O `/export`
 * devolve a conversa uma única vez, sem caixa de input nem barra de status.
 *
 * O caminho de destino vai **cru** no comando: o CLI faz `argumento.trim()` e usa o resultado
 * como nome de arquivo, então aspas POSIX virariam parte do nome. É o oposto de
 * [ClaudeCommand.quote], que protege contra o shell.
 */
object ClaudeSessionExport {

    private val LOG = Logger.getInstance(ClaudeSessionExport::class.java)

    private const val PREFIX = "claude-dock-export-"

    /**
     * Extensão obrigatória: sem ela o CLI acrescenta `.txt` por conta própria e grava em
     * outro caminho, que não saberíamos ler nem apagar.
     */
    private const val SUFFIX = ".md"

    private const val POLL_INTERVAL_MS = 120L
    private const val TIMEOUT_MS = 20_000L

    /**
     * Cria o destino do export.
     *
     * `createTempFile` já nasce com permissão exclusiva do usuário em sistemas POSIX, e o CLI
     * grava por cima sem alterar o modo do arquivo — é o que satisfaz RF-25.
     */
    fun createTarget(): Path = Files.createTempFile(PREFIX, SUFFIX)

    /** Linha digitada na sessão, terminada em CR (equivale ao Enter). */
    fun command(target: Path): String = "/export ${target.toAbsolutePath()}\r"

    /**
     * Espera o CLI gravar o arquivo e devolve o conteúdo normalizado.
     *
     * Devolve `null` se o prazo estourar ou o arquivo continuar vazio (CB-29) — a sessão pode
     * estar ocupada, encerrada, ou o slash command pode ter mudado de nome (R-11).
     *
     * ponytail: sondagem simples em vez de WatchService. O arquivo é escrito de uma vez
     * (`writeFile` com flush), então basta esperar deixar de estar vazio; trocar por
     * WatchService só se o prazo virar problema.
     */
    fun awaitContent(
        target: Path,
        timeoutMs: Long = TIMEOUT_MS,
        pollIntervalMs: Long = POLL_INTERVAL_MS,
    ): String? {
        val deadline = System.currentTimeMillis() + timeoutMs

        while (System.currentTimeMillis() < deadline) {
            readIfReady(target)?.let { return it }

            try {
                Thread.sleep(pollIntervalMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return null
            }
        }

        // Uma última tentativa: o arquivo pode ter chegado dentro do intervalo final.
        return readIfReady(target)
    }

    /** Remove o destino, com ou sem sucesso da cópia (RF-25). */
    fun delete(target: Path) {
        try {
            Files.deleteIfExists(target)
        } catch (e: IOException) {
            // Não é motivo para incomodar o usuário: só um temporário sobrando.
            LOG.warn("Não foi possível apagar o arquivo temporário do export", e)
        }
    }

    private fun readIfReady(target: Path): String? {
        return try {
            if (Files.size(target) == 0L) return null
            ClaudeSessionText.normalize(Files.readString(target))
        } catch (e: IOException) {
            // Arquivo ainda não existe ou está sendo escrito: segue esperando.
            null
        }
    }
}
