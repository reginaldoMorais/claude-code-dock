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

    /** Respiro entre o conteúdo da sessão e as bordas da janela, em pixels lógicos. */
    var sessionPadding: Int = DEFAULT_PADDING

    /** Tecla que representa Esc (mantida por compatibilidade, sempre Backspace agora). */
    @Deprecated("Sempre Backspace em GoLand; Esc em IntelliJ")
    var escapeKeyName: String = "BACKSPACE"


    override fun getState(): ClaudeDockSettings = this

    override fun loadState(state: ClaudeDockSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    /** Caminho efetivo: cai no padrão se o usuário limpar o campo. */
    fun effectiveExecutable(): String =
        claudeExecutable.trim().ifEmpty { DEFAULT_EXECUTABLE }

    /**
     * Respiro efetivo, limitado à faixa aceita.
     *
     * O spinner da tela já limita, mas o XML é editável à mão: um valor absurdo aqui comeria
     * as colunas do terminal.
     */
    fun effectivePadding(): Int = sessionPadding.coerceIn(MIN_PADDING, MAX_PADDING)

    companion object {
        const val DEFAULT_EXECUTABLE = "claude"
        const val DEFAULT_PADDING = 20
        const val MIN_PADDING = 0
        const val MAX_PADDING = 48

        fun getInstance(): ClaudeDockSettings =
            ApplicationManager.getApplication().getService(ClaudeDockSettings::class.java)
    }
}
