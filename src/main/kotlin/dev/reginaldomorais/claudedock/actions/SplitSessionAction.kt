package dev.reginaldomorais.claudedock.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import dev.reginaldomorais.claudedock.ClaudeDockSessions

/**
 * Menu "Dividir" no cabeçalho da tool window (RF-36).
 *
 * O menu de contexto do próprio terminal já oferece "Split Right"/"Split Down" desde que o
 * listener esteja instalado (D-33), então este menu é um **segundo caminho** para a mesma ação —
 * exatamente o que reprovou o RF-30 em v1.5.1. A diferença é de precedente, não de critério: o
 * RF-26 já foi aceito só por descoberta ("`Ctrl+C` copia, mas é invisível para quem usa o
 * mouse"), e o menu de contexto do terminal é tão invisível quanto. Decisão do usuário, com o
 * conflito registrado em D-35.
 */
class SplitSessionMenuAction : DefaultActionGroup("Dividir", true), DumbAware {

    init {
        templatePresentation.icon = AllIcons.Actions.SplitVertically
        templatePresentation.description = "Divide a aba em duas sessões do Claude Code"

        addAll(
            SplitSessionAction(
                "À direita",
                "Abre uma sessão ao lado da atual",
                AllIcons.Actions.SplitVertically,
                stacked = false,
            ),
            SplitSessionAction(
                "Abaixo",
                "Abre uma sessão abaixo da atual",
                AllIcons.Actions.SplitHorizontally,
                stacked = true,
            ),
        )
        addSeparator()
        add(CloseSplitAction())
    }
}

/**
 * Fecha a divisão em foco, preservando a aba (RF-41).
 *
 * O menu de contexto do terminal já oferece isto desde a v1.7, sob o rótulo "Close Tab" — nome
 * da plataforma, que numa aba dividida diz o oposto do que faz. Este item existe para que a
 * capacidade tenha um nome honesto e fique onde o usuário procura.
 */
class CloseSplitAction : AnAction(
    "Fechar divisão",
    "Fecha a sessão em foco e devolve o espaço à sessão vizinha",
    AllIcons.Actions.Cancel,
), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeDockSessions.getInstance(project).closeSelectedSplit()
    }
}

/**
 * Divide a sessão em foco (RF-36).
 *
 * @param stacked `true` empilha as sessões; `false` põe lado a lado.
 */
class SplitSessionAction(
    text: String,
    description: String,
    icon: javax.swing.Icon,
    private val stacked: Boolean,
) : AnAction(text, description, icon), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeDockSessions.getInstance(project).splitSelectedSession(stacked)
    }
}
