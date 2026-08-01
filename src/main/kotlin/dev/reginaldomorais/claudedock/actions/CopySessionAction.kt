package dev.reginaldomorais.claudedock.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import dev.reginaldomorais.claudedock.ClaudeDockSessions

/** Copia o conteúdo da sessão selecionada para a área de transferência (RF-21). */
class CopySessionAction : AnAction(
    "Copiar Sessão",
    "Copia o conteúdo da sessão selecionada para a área de transferência",
    AllIcons.Actions.Copy,
), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeDockSessions.getInstance(project).copySelectedSession()
    }
}
