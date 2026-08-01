package dev.reginaldomorais.claudedock.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Configuração por projeto — ou seja, por janela do IDE (RF-19).
 *
 * Guarda o `CLAUDE_CONFIG_DIR` a ser injetado nas sessões deste projeto. Fica no arquivo de
 * workspace (não versionado) porque é um caminho local da máquina, e o diretório apontado
 * contém credenciais do CLI (RNF-05).
 */
@Service(Service.Level.PROJECT)
@State(
    name = "ClaudeDockProjectSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class ClaudeDockProjectSettings : PersistentStateComponent<ClaudeDockProjectSettings> {

    /** Vazio significa "não injetar": o CLI usa o padrão dele (`~/.claude`). */
    var claudeConfigDir: String = ""

    override fun getState(): ClaudeDockProjectSettings = this

    override fun loadState(state: ClaudeDockProjectSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    /** Valor efetivo, ou `null` quando o usuário não configurou nada. */
    fun effectiveConfigDir(): String? = claudeConfigDir.trim().ifEmpty { null }

    companion object {
        fun getInstance(project: Project): ClaudeDockProjectSettings = project.service()
    }
}
