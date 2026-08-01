package dev.reginaldomorais.claudedock.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import dev.reginaldomorais.claudedock.ClaudeDockSessions

/** Dispara o `/export` do CLI na sessão selecionada, que produz a transcrição (RF-22). */
class ExportSessionAction : AnAction(
    "Exportar Conversa",
    "Executa /export na sessão selecionada para exportar a conversa",
    AllIcons.ToolbarDecorator.Export,
), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeDockSessions.getInstance(project).exportSelectedSession()
    }
}
