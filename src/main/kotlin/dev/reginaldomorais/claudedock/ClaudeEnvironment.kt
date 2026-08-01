package dev.reginaldomorais.claudedock

import com.intellij.openapi.util.io.OSAgnosticPathUtil

/**
 * Monta o ambiente extra entregue à sessão (RF-19).
 *
 * Lógica pura e sem dependência de `Project`, para poder ser testada sem subir um IDE.
 * O mapa é aditivo: a plataforma o funde com o ambiente do shell e com o que os
 * `LocalTerminalCustomizer` injetam — inclusive o `CLAUDE_CODE_SSE_PORT` do plugin oficial.
 */
object ClaudeEnvironment {

    const val CONFIG_DIR_VAR = "CLAUDE_CONFIG_DIR"

    /**
     * Traduz o diretório configurado em variáveis de ambiente.
     *
     * Valor ausente ou em branco produz mapa vazio, deixando o CLI usar o padrão dele.
     * O `~` é expandido aqui porque variável de ambiente não passa por expansão do shell.
     */
    fun build(configDir: String?): Map<String, String> {
        val trimmed = configDir?.trim().orEmpty()
        if (trimmed.isEmpty()) return emptyMap()

        return mapOf(CONFIG_DIR_VAR to OSAgnosticPathUtil.expandUserHome(trimmed))
    }
}
