package dev.reginaldomorais.claudedock.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import dev.reginaldomorais.claudedock.ClaudeDockSessions

/**
 * Envia `/usage` à sessão em foco (RF-51).
 *
 * Passa pelo serviço, como todas as ações do cabeçalho — foi tentar atalhar pelo `DataContext`
 * que deixou "Tocar seleção" mudo por quatro rodadas (DEF-07).
 */
class UsageSessionAction : AnAction(
    "Uso",
    "Mostra o consumo do Claude Code na sessão em foco",
    // Cinza neutro, como as irmãs do cabeçalho. `Profile` é o medidor monocromático — as
    // variantes coloridas da família chamam-se ProfileBlue/Red/Yellow.
    AllIcons.Actions.Profile,
), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeDockSessions.getInstance(project).openUsage()
    }
}
