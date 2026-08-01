package dev.reginaldomorais.claudedock.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Configuração persistida do plugin.
 *
 * Guarda apenas o caminho do executável. Nenhum segredo é armazenado (RNF-05).
 */
@State(
    name = "ClaudeDockSettings",
    storages = [Storage("claude-code-dock.xml")],
)
class ClaudeDockSettings : PersistentStateComponent<ClaudeDockSettings> {

    /** Caminho do CLI. O padrão resolve pelo PATH. */
    var claudeExecutable: String = DEFAULT_EXECUTABLE

    override fun getState(): ClaudeDockSettings = this

    override fun loadState(state: ClaudeDockSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    /** Caminho efetivo: cai no padrão se o usuário limpar o campo. */
    fun effectiveExecutable(): String =
        claudeExecutable.trim().ifEmpty { DEFAULT_EXECUTABLE }

    companion object {
        const val DEFAULT_EXECUTABLE = "claude"

        fun getInstance(): ClaudeDockSettings =
            ApplicationManager.getApplication().getService(ClaudeDockSettings::class.java)
    }
}
