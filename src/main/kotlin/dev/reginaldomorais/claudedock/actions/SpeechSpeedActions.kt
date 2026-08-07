package dev.reginaldomorais.claudedock.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings

/**
 * Submenu "Velocidade" dentro do menu "Áudio" (RF-47).
 *
 * Atalho para o mesmo campo que a tela de Settings edita, e sobre a mesma tabela
 * (`ClaudeDockSettings.SPEECH_SPEEDS`) — não há estado nem lista paralela aqui.
 *
 * A velocidade vale para a **próxima** fala: o piper sintetiza o áudio inteiro antes de tocar,
 * então mudar o valor no meio de uma reprodução não a altera (Q-29).
 */
class SpeechSpeedMenuAction : DefaultActionGroup("Velocidade", true), DumbAware {

    init {
        templatePresentation.description = "Velocidade da fala do Piper"
        ClaudeDockSettings.SPEECH_SPEEDS.keys.forEach { add(SpeechSpeedAction(it)) }
    }

    /**
     * Lê apenas configuração de aplicação, não a árvore de componentes.
     *
     * O `AudioMenuAction` — o menu que hospeda este — declara o seu pelo mesmo motivo: sem
     * declarar, um submenu já apareceu vazio uma vez (DEF-04).
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /** Mostra a velocidade em vigor no próprio rótulo, sem precisar abrir o submenu. */
    override fun update(e: AnActionEvent) {
        val current = ClaudeDockSettings.getInstance().effectiveSpeechSpeed()
        e.presentation.text = "Velocidade (${ClaudeDockSettings.speechSpeedLabel(current)})"
    }
}

/**
 * Uma velocidade do submenu (RF-47).
 *
 * `ToggleAction` desenha um ✔, não um radio — é o idioma da plataforma para opção de menu.
 * Só um fica marcado porque `setSelected` grava um valor único.
 */
class SpeechSpeedAction(private val percent: Int) :
    ToggleAction(ClaudeDockSettings.SPEECH_SPEEDS.getValue(percent)), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun isSelected(e: AnActionEvent): Boolean =
        ClaudeDockSettings.getInstance().effectiveSpeechSpeed() == percent

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        // Desmarcar não tem significado: alguma velocidade sempre vale.
        if (state) ClaudeDockSettings.getInstance().speechSpeed = percent
    }
}
