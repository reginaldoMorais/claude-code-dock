package dev.reginaldomorais.claudedock.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import dev.reginaldomorais.claudedock.ClaudeDockSessions

/**
 * Menu "Sessões" no cabeçalho da tool window (RF-36).
 *
 * _(v1.12)_ **Chamava-se "Dividir" e o nome mentia** — dos oito itens, cinco não dividem nada:
 * renomear a aba, renomear a sessão, fechar a divisão, fechar todas e os dois de reposicionar.
 * Q-34 previa decidir isto depois dos roteiros manuais, e o F3 decidiu: o usuário não achou o
 * rename, porque ninguém procura "renomear" dentro de "Dividir". É o mesmo defeito do DEF-06, em
 * que "Fechar divisão" foi lido como "fechar as divisões" — nome de menu que descreve um item em
 * vez do conjunto.
 *
 * O menu de contexto do próprio terminal já oferece "Split Right"/"Split Down" desde que o
 * listener esteja instalado (D-33), então este menu é um **segundo caminho** para a mesma ação —
 * exatamente o que reprovou o RF-30 em v1.5.1. A diferença é de precedente, não de critério: o
 * RF-26 já foi aceito só por descoberta ("`Ctrl+C` copia, mas é invisível para quem usa o
 * mouse"), e o menu de contexto do terminal é tão invisível quanto. Decisão do usuário, com o
 * conflito registrado em D-35.
 */
class SplitSessionMenuAction : DefaultActionGroup("Sessões", true), DumbAware {

    init {
        templatePresentation.icon = AllIcons.Actions.SplitVertically
        templatePresentation.description =
            "Divide, reposiciona, renomeia e fecha as sessões do Claude Code"

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
        addAll(
            RearrangeSplitAction(
                "Trocar de lado",
                "Troca a sessão em foco de lugar com a vizinha",
                AllIcons.Actions.SwapPanels,
            ) { it.swapSelectedSplit() },
            RearrangeSplitAction(
                "Girar divisão",
                "Alterna entre lado a lado e empilhado",
                AllIcons.Actions.SynchronizeScrolling,
            ) { it.rotateSelectedSplit() },
        )
        addSeparator()
        // _(v1.12)_ Renomear entra aqui, e não num ícone novo do cabeçalho: o menu já é o lugar
        // onde "esta sessão" e "a aba" são coisas distintas, que é a distinção que o rename faz.
        // O nome "Dividir" fica sob medida errada com isso — registrado em Q-34.
        addAll(
            RenameTabMenuAction(),
            RenamePaneMenuAction(),
        )
        addSeparator()
        add(CloseSplitAction())
        add(CloseAllSessionsAction())
    }

    /**
     * Lê o estado da árvore de componentes, que é da EDT.
     *
     * O `AudioMenuAction` — o outro menu deste cabeçalho, e o que comprovadamente funciona —
     * declara o seu explicitamente. Este não declarava, e o menu apareceu vazio uma vez até o
     * usuário trocar de aba (DEF-04).
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        // "À direita" e "Abaixo" valem sempre, então o grupo nunca fica sem filho habilitado —
        // é isso que impede `isDisableGroupIfEmpty` de apagar o menu inteiro.
        e.presentation.isEnabled = e.project != null
    }
}

/**
 * Reposiciona a divisão em foco (RF-43).
 *
 * Trocar de lado e girar cobrem o que "reposicionar" significa numa aba de duas a quatro panes.
 * Arrastar com o mouse continua fora de escopo, e não por dificuldade de DnD: a superfície da
 * pane já é do terminal — arrastar ali **é** selecionar texto (RF-26) —, então o arraste exigiria
 * primeiro uma barra de título por pane, roubando altura de todas para servir uma ação
 * ocasional. Ver Q-28.
 */
class RearrangeSplitAction(
    text: String,
    description: String,
    icon: javax.swing.Icon,
    private val rearrange: (ClaudeDockSessions) -> Unit,
) : SplitOnlyAction(text, description, icon) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        rearrange(ClaudeDockSessions.getInstance(project))
    }
}

/**
 * Fecha a divisão em foco, preservando a aba (RF-41).
 *
 * O menu de contexto do terminal já oferece isto desde a v1.7, sob o rótulo "Close Tab" — nome
 * da plataforma, que numa aba dividida diz o oposto do que faz. Este item existe para que a
 * capacidade tenha um nome honesto e fique onde o usuário procura.
 */
class CloseSplitAction : SplitOnlyAction(
    "Fechar esta sessão",
    "Fecha apenas a sessão em foco e devolve o espaço à vizinha",
    AllIcons.Actions.Cancel,
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeDockSessions.getInstance(project).closeSelectedSplit()
    }
}

/**
 * Fecha a aba e todas as sessões dela (RF-46).
 *
 * O `X` da aba já fazia isso, mas quem está no menu de divisões procura por ali — e foi
 * justamente a ausência deste item que fez "Fechar divisão" ser lido como "fechar todas"
 * (DEF-06). Os dois nomes agora dizem quantas sessões morrem.
 */
class CloseAllSessionsAction : AnAction(
    "Fechar todas as sessões",
    "Fecha a aba inteira, com todas as sessões divididas",
    AllIcons.Actions.CloseHovered,
), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeDockSessions.getInstance(project).closeSelectedTab()
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

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeDockSessions.getInstance(project).splitSelectedSession(stacked)
    }
}

/**
 * Ação que só faz sentido com a aba dividida (RF-45).
 *
 * Desabilitar é melhor que avisar depois do clique: o usuário vê antes de tentar. As
 * notificações continuam nos métodos do serviço, porque o estado pode mudar entre o `update` e
 * o clique.
 */
abstract class SplitOnlyAction(
    text: String,
    description: String,
    icon: javax.swing.Icon,
) : AnAction(text, description, icon), DumbAware {

    /** Lê a árvore de componentes, que é da EDT. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled =
            project != null && ClaudeDockSessions.getInstance(project).isSelectedSessionSplit()
    }
}
