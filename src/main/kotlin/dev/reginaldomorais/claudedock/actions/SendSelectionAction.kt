package dev.reginaldomorais.claudedock.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import dev.reginaldomorais.claudedock.ClaudeDockSessions
import dev.reginaldomorais.claudedock.ClaudeEditorReference

/**
 * Envia o trecho selecionado no editor para a sessão em foco da janela dedicada (RF-49).
 *
 * Contraparte do `Ctrl+Alt+K` do plugin oficial, que não alcança esta janela (DEF-08). Sem atalho
 * padrão de propósito: definir um mexeria no keymap do usuário, o que RF-13 proíbe — e o único
 * atalho "natural" já é do oficial. Quem quiser liga um em _Settings > Keymap_.
 */
class SendSelectionAction : AnAction(
    "Enviar Seleção para o Claude Code",
    "Envia o trecho selecionado no editor para a sessão em foco da janela dedicada",
    AllIcons.Actions.MoveTo2,
), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val hasFile = e.getData(CommonDataKeys.VIRTUAL_FILE) != null
        e.presentation.isEnabledAndVisible = e.project != null && hasFile
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val editor = e.getData(CommonDataKeys.EDITOR)

        val relativePath = ClaudeEditorReference.relativize(project.basePath, file.path)

        // Sem editor (aba de projeto, por exemplo) a referência é só o arquivo — a mesma
        // degradação que o CLI faz quando a notificação vem sem linhas.
        val reference = if (editor == null || !editor.selectionModel.hasSelection()) {
            ClaudeEditorReference.format(relativePath, 0, 0)
        } else {
            val document = editor.document
            val selection = editor.selectionModel
            val startLine = document.getLineNumber(selection.selectionStart) + 1
            val rawEndLine = document.getLineNumber(selection.selectionEnd) + 1
            val endLine = ClaudeEditorReference.inclusiveEndLine(
                startLine,
                rawEndLine,
                endsAtLineStart = selection.selectionEnd == document.getLineStartOffset(rawEndLine - 1),
            )
            ClaudeEditorReference.format(relativePath, startLine, endLine)
        }

        ClaudeDockSessions.getInstance(project).sendEditorReference(reference)
    }
}
