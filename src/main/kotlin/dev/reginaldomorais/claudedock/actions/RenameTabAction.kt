package dev.reginaldomorais.claudedock.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.ToolWindowTabRenameActionBase
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import dev.reginaldomorais.claudedock.ClaudeDockSessions

/**
 * Renomeia a aba pela edição in-place no próprio rótulo (RF-58).
 *
 * **Nenhuma linha de UI é nossa.** `ToolWindowTabRenameActionBase` já monta o campo de edição sobre
 * o rótulo, posiciona o popup e cuida do ciclo de vida dele; o `update` da base habilita a ação
 * comparando `toolWindow.id` com o que passamos no construtor. É a mesma forma do D-33: o
 * mecanismo já existia na plataforma e faltava registrá-lo — desta vez com o precedente literal do
 * Terminal, que registra `Terminal.RenameSession` no mesmo grupo `ToolWindowContextMenu` (D-53).
 *
 * Funciona também com **uma aba só**, quando o cabeçalho mostra o título da janela e não há rótulo
 * visível: a base cai de `CONTEXT_COMPONENT` para `ToolWindowContentUi.SELECTED_CONTENT_TAB_LABEL`
 * (CB-73).
 *
 * A classe vive em `intellij.platform.ide.impl` e não tem contrato de estabilidade (R-31). A
 * capacidade **degrada, não morre**: [RenameTabMenuAction] faz o mesmo pelo cabeçalho, sem
 * depender dela.
 */
class RenameTabAction : ToolWindowTabRenameActionBase(
    ClaudeDockSessions.TOOL_WINDOW_ID,
    ClaudeDockSessions.RENAME_TAB_LABEL,
) {

    /**
     * O campo abre com o nome **base**, sem o sufixo de estado.
     *
     * Quem edita edita o nome, não o estado. Devolver o `displayName` cru faria a edição de uma
     * aba encerrada abrir com "(encerrado)" dentro, e confirmar sem mexer gravaria o sufixo como
     * parte do nome — o "(encerrado) (encerrado)" que `TAB_TITLE` sempre existiu para evitar.
     */
    override fun getContentDisplayNameToEdit(content: Content, project: Project): String =
        ClaudeDockSessions.baseTitleOf(content)

    override fun applyContentDisplayName(
        content: Content,
        project: Project,
        newContentName: String,
    ) {
        ClaudeDockSessions.getInstance(project).applyTabName(content, newContentName)
    }
}

/**
 * Renomeia a aba pelo menu do cabeçalho (RF-58).
 *
 * Segundo caminho para a mesma capacidade, o que o RF-30 foi recusado por ser em v1.5.1. A
 * diferença é o **alcance do primeiro caminho**, não a contagem: lá o primeiro era o menu "Áudio"
 * do nosso próprio cabeçalho, visível e já usado; aqui é o menu de contexto da aba, onde o plugin
 * nunca pôs nada — ninguém clica ali procurando função nova (Achado 29, Achado 44).
 */
class RenameTabMenuAction : AnAction(
    "Renomear a Aba…",
    "Dá um nome à aba, no lugar do automático",
    AllIcons.Actions.Edit,
), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeDockSessions.getInstance(project).renameSelectedTab()
    }
}

/**
 * Dá nome à sessão em foco — o subtítulo da pane (RF-60).
 *
 * Não é [SplitOnlyAction]: nomear vale mesmo sem divisão, porque o subtítulo mora na faixa de
 * respiro, que existe com uma pane ou com quatro.
 */
class RenamePaneMenuAction : AnAction(
    "Renomear Esta Sessão…",
    "Escreve um nome na faixa desta sessão, sem roubar altura do terminal",
    AllIcons.Actions.Edit,
), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeDockSessions.getInstance(project).renameSelectedPane()
    }
}
