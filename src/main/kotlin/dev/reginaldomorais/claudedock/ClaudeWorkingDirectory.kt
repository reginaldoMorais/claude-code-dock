package dev.reginaldomorais.claudedock

import com.intellij.util.SystemProperties

/**
 * Resolve o diretório de trabalho da sessão.
 *
 * Recebe o `basePath` em vez do `Project` para permanecer puro e testável (CB-05).
 */
object ClaudeWorkingDirectory {

    fun resolve(basePath: String?): String =
        basePath?.takeIf { it.isNotBlank() } ?: SystemProperties.getUserHome()
}
