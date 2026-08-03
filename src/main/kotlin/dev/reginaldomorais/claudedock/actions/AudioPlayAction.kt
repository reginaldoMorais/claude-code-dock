package dev.reginaldomorais.claudedock.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.terminal.JBTerminalWidget
import dev.reginaldomorais.claudedock.ClaudePiperPlayback
import dev.reginaldomorais.claudedock.ClaudeTtaSessions
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings

/**
 * Toca o texto selecionado na sessão ativa (RF-30).
 */
class AudioPlayAction : AnAction("Tocar seleção", "Tocar texto selecionado via Piper", AllIcons.Actions.Execute), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        // Piper disponível?
        val canPlay = ClaudePiperPlayback.canSynthesize(
            ClaudeDockSettings.getInstance().effectivePiperExecutable(),
            ClaudeDockSettings.getInstance().effectivePiperModel(),
        )

        e.presentation.isEnabled = canPlay
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Tenta obter o widget do contexto
        val widget = e.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.CONTEXT_COMPONENT)
            as? JBTerminalWidget
            ?: return

        // Pega o texto selecionado
        val selectedText = widget.selectedText ?: return
        if (selectedText.isBlank()) return

        // Toca o texto
        ClaudeTtaSessions.getInstance(project).playText(selectedText)
    }
}
