package dev.reginaldomorais.claudedock

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import dev.reginaldomorais.claudedock.settings.ClaudeDockConfigurable
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * Gerencia as abas de sessão dentro da tool window dedicada.
 *
 * Não conhece detalhes da API de terminal — isso é responsabilidade de
 * [ClaudeTerminalSessionFactory] (RNF-14).
 */
@Service(Service.Level.PROJECT)
class ClaudeDockSessions(private val project: Project) :
    ClaudeTerminalSessionFactory.SessionHost {

    /** Guarda contra dois `/export` simultâneos na mesma janela (CB-32). */
    private val exportInProgress = AtomicBoolean(false)

    /** Sessão nova (RF-07). Único ponto que monta o comando, para não duplicar a regra. */
    override fun openNewSession() = ClaudeDockSettings.getInstance().let {
        openSession(ClaudeCommand.newSession(it.effectiveExecutable()))
    }

    /** Retomada de conversa anterior (RF-08). */
    fun openResumeSession() = ClaudeDockSettings.getInstance().let {
        openSession(ClaudeCommand.resumeSession(it.effectiveExecutable()))
    }

    /**
     * Abre uma sessão em nova aba, ativando a tool window.
     *
     * A verificação do executável roda fora da EDT (RNF-03); a sessão é aberta de
     * qualquer forma, deixando um shell utilizável para diagnóstico (Fluxo D).
     */
    fun openSession(command: String) {
        val executable = ClaudeDockSettings.getInstance().effectiveExecutable()

        ApplicationManager.getApplication().executeOnPooledThread {
            val available = ClaudeTerminalSessionFactory.isExecutableAvailable(executable)

            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                if (!available) notifyExecutableMissing(executable)

                val toolWindow = findToolWindow() ?: return@invokeLater
                toolWindow.activate {
                    addSession(toolWindow.contentManager, command)
                }
            }
        }
    }

    /**
     * Cria a aba de sessão e a adiciona ao [contentManager].
     *
     * Falha ao criar não derruba a tool window (RNF-10).
     */
    fun addSession(contentManager: ContentManager, command: String) {
        val title = nextTabTitle(contentManager)

        // Disposable da aba: fechar a aba encerra **todas** as suas sessões (RNF-09, RNF-28).
        val tabDisposable = Disposer.newDisposable("ClaudeDockTab")

        val pane = createPane(tabDisposable, command) ?: run {
            Disposer.dispose(tabDisposable)
            return
        }

        val content: Content = ContentFactory.getInstance()
            // O painel raiz é o que dá onde encaixar o splitter na primeira divisão (RF-36).
            .createContent(ClaudeSessionSplitter.root(pane.component), title, false)
        content.isCloseable = true
        content.setDisposer(tabDisposable)
        // O foco vai para o terminal, e não para o painel que o cobre. A referência vive junto com
        // a aba: fechar a aba a leva embora, sem mapa para limpar.
        pointTabAt(content, pane.widget)

        content.putUserData(TAB_TITLE, title)

        contentManager.addContent(content)
        contentManager.setSelectedContent(content)

        markEndedWhenLast(content, pane)
    }

    /**
     * Marca a aba como encerrada quando a sessão morre — mas só quando isso significa o que diz
     * (RF-11, RF-44).
     *
     * O callback é registrado **por pane**, e não só para a sessão original da aba, com duas
     * guardas que DEF-03 mostrou serem necessárias:
     *
     * - **A pane ainda está na árvore.** Fechar a divisão tira a pane e só depois mata o
     *   processo; sem esta guarda, fechar deliberadamente marcava a aba de "encerrado" com uma
     *   sessão viva do lado.
     * - **É a última pane.** Numa aba dividida, uma sessão que acaba não encerra a aba — a
     *   vizinha continua trabalhando. Isto responde Q-26 com o que o uso real mostrou.
     */
    private fun markEndedWhenLast(content: Content, pane: Pane) {
        pane.widget.addTerminationCallback({
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                // Saiu da árvore: foi fechamento deliberado, não sessão encerrada.
                if (ClaudeSessionSplitter.paneOf(pane.component, content.component) == null) {
                    return@invokeLater
                }
                if (ClaudeSessionSplitter.countPanes(content.component) > 1) return@invokeLater

                // O estado vira dado da aba, e não string interpolada: é ele que permite ao rename
                // recompor o rótulo sem perder o sufixo, e ao sufixo não apagar o nome (RF-59).
                content.putUserData(TAB_ENDED, true)
                content.displayName = ClaudeTabTitle.display(baseTitleOf(content), ended = true)
            }
        }, pane.disposable)
    }

    /**
     * Único escritor do nome da aba (D-55).
     *
     * As duas entradas — a edição in-place do menu de contexto e o diálogo do cabeçalho — passam
     * por aqui, para não existirem duas regras de composição que divergem com o tempo.
     */
    fun applyTabName(content: Content, name: String) {
        val fallback = content.manager?.let { nextTabTitle(it, exclude = content) }
            ?: ClaudeTabTitle.BASE
        // Em branco significa "volte ao padrão", e não erro (RF-62).
        val base = name.trim().ifEmpty { fallback }

        content.putUserData(TAB_TITLE, base)
        content.displayName =
            ClaudeTabTitle.display(base, ended = content.getUserData(TAB_ENDED) == true)
    }

    /**
     * Renomeia a aba selecionada pelo diálogo do cabeçalho (RF-58).
     *
     * É o segundo caminho do rename; o primeiro é a edição in-place no rótulo, que a plataforma
     * entrega pronta. Existe porque o plugin nunca pôs nada no menu de contexto da aba, e ninguém
     * clica ali procurando função nova (Achado 29, Achado 44).
     */
    fun renameSelectedTab() {
        val content = findToolWindow()?.contentManager?.selectedContent
            ?: return notify("Nenhuma aba aberta para renomear.", NotificationType.WARNING)

        // Cancelar devolve `null` e não altera nada; em branco é intenção de apagar (RF-62).
        val name = Messages.showInputDialog(
            project,
            RENAME_TAB_LABEL,
            "Renomear Aba",
            null,
            baseTitleOf(content),
            null,
        ) ?: return

        applyTabName(content, name)
    }

    /**
     * Dá nome à sessão em foco — o subtítulo da pane (RF-60).
     *
     * Resolve a pane pelo mesmo caminho de "Fechar esta sessão", então respeita o foco sem saber
     * que o split existe (RF-38, D-34).
     */
    fun renameSelectedPane() {
        val widget = selectedWidget()
            ?: return notify("Nenhuma sessão aberta para renomear.", NotificationType.WARNING)
        val content = contentOf(widget) ?: return

        val name = Messages.showInputDialog(
            project,
            "Nome desta sessão:",
            "Renomear Sessão",
            null,
            ClaudeSessionPadding.subtitleOf(widget.component).orEmpty(),
            null,
        ) ?: return

        // A pane pode ter sido fechada enquanto o diálogo estava aberto: escrever nela gravaria
        // num componente que ninguém mais pinta (CB-72).
        if (ClaudeSessionSplitter.paneOf(widget.component, content.component) == null) return

        ClaudeSessionPadding.setSubtitle(widget.component, name)
    }


    /**
     * Divide a pane de [widget], abrindo uma sessão nova ao lado (RF-36).
     *
     * A nova pane é filha do disposable da **aba**, e não da pane de origem: fechar a divisão de
     * cima não pode arrastar a de baixo junto (RNF-28).
     */
    override fun splitSession(widget: TerminalWidget, stacked: Boolean) {
        val content = contentOf(widget)
            ?: return notify("Nenhuma sessão aberta para dividir.", NotificationType.WARNING)
        val tabDisposable = content.disposer ?: return
        val existing = ClaudeSessionSplitter.paneOf(widget.component, content.component) ?: return

        val command = ClaudeCommand.newSession(
            ClaudeDockSettings.getInstance().effectiveExecutable(),
        )
        val incoming = createPane(tabDisposable, command) ?: return

        if (!ClaudeSessionSplitter.split(existing, incoming.component, stacked)) {
            Disposer.dispose(incoming.disposable)
            return notify("Não foi possível dividir a sessão.", NotificationType.WARNING)
        }

        // A pane nova também marca a aba quando for a última a sobrar (RF-44).
        markEndedWhenLast(content, incoming)

        // Divisão sem rótulo nenhum deixa o usuário sem saber qual pane o menu atinge, e esconde
        // que dá para nomeá-las (DEF-11). O nome automático é ponto de partida: sobrescrever é um
        // rename, apagar é um rename em branco (RF-62).
        if (ClaudeSessionPadding.subtitleOf(widget.component) == null) {
            ClaudeSessionPadding.setSubtitle(widget.component, ClaudeSessionPadding.autoLabel(1))
        }
        ClaudeSessionPadding.setSubtitle(
            incoming.widget.component,
            ClaudeSessionPadding.autoLabel(ClaudeSessionSplitter.countPanes(content.component)),
        )

        // A divisão nasce com o foco: quem dividiu quer digitar na sessão nova.
        incoming.widget.requestFocus()
    }

    /** Se a sessão em foco está numa divisão — habilita as ações que só fazem sentido aí. */
    fun isSelectedSessionSplit(): Boolean {
        val widget = selectedWidget() ?: return false
        val content = contentOf(widget) ?: return false
        val pane = ClaudeSessionSplitter.paneOf(widget.component, content.component) ?: return false

        return ClaudeSessionSplitter.isSplit(pane)
    }

    /** Divide a sessão em foco da aba selecionada — o caminho do cabeçalho (RF-36). */
    fun splitSelectedSession(stacked: Boolean) {
        val widget = selectedWidget()
            ?: return notify("Nenhuma sessão aberta para dividir.", NotificationType.WARNING)

        splitSession(widget, stacked)
    }

    /**
     * Fecha a pane de [widget], colapsando a divisão (RF-39).
     *
     * Sendo a única sessão da aba, fechar a sessão é fechar a aba — que é o que o item "Close
     * Tab" do menu de contexto significa quando não há divisão.
     */
    override fun closeSession(widget: TerminalWidget) {
        if (closeSplit(widget)) return

        contentOf(widget)?.let { findToolWindow()?.contentManager?.removeContent(it, true) }
    }

    /** Troca a sessão em foco de lado com a vizinha (RF-43). */
    fun swapSelectedSplit() = rearrangeSelectedSplit(ClaudeSessionSplitter::swap)

    /** Gira a divisão em foco: lado a lado ↔ empilhado (RF-43). */
    fun rotateSelectedSplit() = rearrangeSelectedSplit(ClaudeSessionSplitter::rotate)

    /**
     * Aplica [rearrange] à divisão da sessão em foco, avisando quando não há divisão.
     *
     * As duas ações diferem só nessa função: separar em dois métodos duplicaria a busca da pane
     * e a mensagem de aviso.
     */
    private fun rearrangeSelectedSplit(rearrange: (JComponent) -> Boolean) {
        val widget = selectedWidget()
            ?: return notify("Nenhuma sessão aberta.", NotificationType.WARNING)
        val content = contentOf(widget) ?: return
        val pane = ClaudeSessionSplitter.paneOf(widget.component, content.component) ?: return

        if (!rearrange(pane)) {
            notify("Esta aba não está dividida.", NotificationType.INFORMATION)
        }
    }

    /**
     * Fecha a divisão em foco pelo cabeçalho, **sem** fechar a aba (RF-41).
     *
     * O menu de contexto do terminal já faz isso desde v1.7, mas sob o rótulo "Close Tab", que
     * é da plataforma e sugere o oposto do que faz numa aba dividida. Daí o item próprio.
     */
    fun closeSelectedSplit() {
        val widget = selectedWidget()
            ?: return notify("Nenhuma sessão aberta para fechar.", NotificationType.WARNING)

        if (!closeSplit(widget)) {
            notify("Esta aba não está dividida; feche a aba pelo X.", NotificationType.INFORMATION)
        }
    }

    /**
     * Tira a pane de [widget] da árvore e encerra **só** o processo dela (RNF-28).
     *
     * @return `false` quando não havia divisão a fechar.
     */
    private fun closeSplit(widget: TerminalWidget): Boolean {
        val content = contentOf(widget) ?: return false
        val pane = ClaudeSessionSplitter.paneOf(widget.component, content.component) ?: return false
        val sibling = ClaudeSessionSplitter.close(pane) ?: return false

        // A pane saiu da árvore; o processo dela morre aqui, e só ele.
        paneDisposable(pane)?.let(Disposer::dispose)
        handOverTo(content, sibling)

        return true
    }

    /**
     * Passa a sessão de referência da aba para uma pane sobrevivente (RF-42, DEF-05).
     *
     * `preferredFocusableComponent` é o ponto que DEF-05 expôs: ele é definido na criação da aba
     * e apontava para o componente da pane original. Fechando **essa** pane, a aba passava a
     * apontar para um componente descartado e fora da árvore — o foco não ia a lugar nenhum, o
     * `DataContext` da toolbar ficava sem projeto, e **todo** o cabeçalho parava, inclusive
     * "Nova sessão". Trocar de aba consertava porque o foco caía num componente válido.
     */
    private fun handOverTo(content: Content, sibling: JComponent) {
        val pane = ClaudeSessionSplitter.firstPane(sibling) ?: return
        val survivor = pane.getClientProperty(PANE_WIDGET) as? TerminalWidget ?: return

        pointTabAt(content, survivor)
        survivor.requestFocus()
    }

    /** Fecha a aba selecionada e **todas** as sessões dela (RF-46). */
    fun closeSelectedTab() {
        val contentManager = findToolWindow()?.contentManager ?: return
        val content = contentManager.selectedContent
            ?: return notify("Nenhuma aba aberta para fechar.", NotificationType.WARNING)

        // O `Disposable` da aba é pai de todas as panes: cai a árvore inteira (RF-40).
        contentManager.removeContent(content, true)
    }

    /**
     * Navegação entre abas pedida pelo menu de contexto do terminal.
     *
     * A guarda não é defensiva por precaução: com uma aba só, a plataforma dispara assertion em
     * vez de ignorar o pedido (DEF-02). E o menu mostra os itens sempre, sem nos consultar.
     */
    override fun selectSiblingTab(next: Boolean) {
        val contentManager = findToolWindow()?.contentManager ?: return
        if (!ClaudeTabNavigation.canNavigate(contentManager.contentCount)) return

        if (next) contentManager.selectNextContent() else contentManager.selectPreviousContent()
    }

    /**
     * Registra qual sessão está em foco na aba (RF-38, D-34).
     *
     * Com o split, uma aba tem várias sessões, e as ações do cabeçalho precisam saber sobre qual
     * agir. A chave é a **mesma** de antes: passa a significar "a última sessão com foco nesta
     * aba" em vez de "a única sessão da aba", e por isso `selectedWidget()` não mudou.
     */
    override fun sessionFocused(widget: TerminalWidget) {
        contentOf(widget)?.let { pointTabAt(it, widget) }
    }

    /**
     * Cria uma sessão e o componente que a representa na árvore da aba.
     *
     * Falha ao criar não derruba a tool window nem a aba (RNF-10): notifica e devolve `null`.
     */
    private fun createPane(tabDisposable: Disposable, command: String): Pane? {
        val paneDisposable = Disposer.newDisposable("ClaudeDockPane")
        Disposer.register(tabDisposable, paneDisposable)

        val widget = try {
            ClaudeTerminalSessionFactory.createSession(project, paneDisposable, command, this)
        } catch (e: Exception) {
            Disposer.dispose(paneDisposable)
            LOG.warn("Falha ao criar a sessão do Claude Code", e)
            notify("Não foi possível iniciar a sessão: ${e.message}", NotificationType.ERROR)
            return null
        }

        // Esconde o prompt e o eco do comando enquanto o CLI sobe.
        val component = ClaudeSessionLoading.wrap(
            widget.component,
            paneDisposable,
            "Iniciando o Claude Code…",
        )
        // Propriedades de cliente em vez de mapa: morrem junto com o componente (D-18, D-36).
        ClaudeSessionSplitter.markPane(component)
        component.putClientProperty(PANE_DISPOSABLE, paneDisposable)
        component.putClientProperty(PANE_WIDGET, widget)

        return Pane(widget, component, paneDisposable)
    }

    private fun paneDisposable(pane: JComponent): Disposable? =
        pane.getClientProperty(PANE_DISPOSABLE) as? Disposable

    /** Aba que contém [widget], descoberta pela árvore de componentes — sem mapa a manter. */
    private fun contentOf(widget: TerminalWidget): Content? =
        findToolWindow()?.contentManager?.contents?.firstOrNull {
            SwingUtilities.isDescendingFrom(widget.component, it.component)
        }

    /** Uma sessão e o bloco que a representa na aba. */
    private class Pane(
        val widget: TerminalWidget,
        val component: JComponent,
        val disposable: Disposable,
    )

    /**
     * Toca o trecho selecionado da sessão em foco (RF-48, corrige DEF-07).
     *
     * Passa pelo serviço como todas as outras ações do cabeçalho: a `AudioPlayAction` lia
     * `CONTEXT_COMPONENT`, que numa ação de título é a barra de ferramentas — nunca o terminal.
     */
    fun playSelectedSession() {
        val widget = selectedWidget()
            ?: return notify("Nenhuma sessão aberta para ouvir.", NotificationType.WARNING)

        val selected = ClaudeSessionText.normalize(JBTerminalWidget.asJediTermWidget(widget)?.selectedText)
        if (selected.isNullOrBlank()) {
            return notify("Selecione um trecho da sessão para ouvir.", NotificationType.INFORMATION)
        }

        ClaudeTtaSessions.getInstance(project).playText(selected)
    }

    /**
     * Copia a conversa da aba selecionada para a área de transferência (RF-24).
     *
     * Passa pelo `/export` do CLI, e não pelo buffer do terminal: o buffer traz a conversa
     * repetida, uma cópia por repintura do TUI (DEF-01). Silencioso no sucesso, como qualquer
     * botão de copiar do IDE.
     */
    fun copySelectedSession() {
        val widget = selectedWidget()
            ?: return notify("Nenhuma sessão aberta para copiar.", NotificationType.WARNING)

        // Um /export por vez: dois em paralelo disputariam a mesma sessão (CB-32).
        if (!exportInProgress.compareAndSet(false, true)) {
            return notify("Já há uma cópia em andamento.", NotificationType.INFORMATION)
        }

        val target = try {
            ClaudeSessionExport.createTarget()
        } catch (e: IOException) {
            exportInProgress.set(false)
            LOG.warn("Não foi possível criar o arquivo temporário do export", e)
            return notify("Não foi possível preparar a cópia: ${e.message}", NotificationType.ERROR)
        }

        if (!ClaudeTerminalSessionFactory.sendInput(widget, ClaudeSessionExport.command(target))) {
            ClaudeSessionExport.delete(target)
            exportInProgress.set(false)
            return notify("A sessão ainda não iniciou; tente de novo em instantes.", NotificationType.WARNING)
        }

        // A espera é por disco: nunca na EDT (RNF-03).
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val text = ClaudeSessionExport.awaitContent(target)

                ApplicationManager.getApplication().invokeLater {
                    if (project.isDisposed) return@invokeLater
                    if (text == null) {
                        notify(
                            "Não foi possível obter a conversa. A sessão está respondendo?",
                            NotificationType.WARNING,
                        )
                    } else {
                        CopyPasteManager.copyTextToClipboard(text)
                    }
                }
            } finally {
                // O arquivo carrega a conversa: some com ou sem sucesso (RF-25, R-14).
                ClaudeSessionExport.delete(target)
                exportInProgress.set(false)
            }
        }
    }

    /**
     * Pede ao próprio CLI a transcrição da conversa, via slash command `/export` (RF-22).
     *
     * O CR final equivale ao Enter: quem apresenta as opções de destino é o Claude Code.
     */
    fun exportSelectedSession() {
        val widget = selectedWidget()
            ?: return notify("Nenhuma sessão aberta para exportar.", NotificationType.WARNING)

        if (!ClaudeTerminalSessionFactory.sendInput(widget, EXPORT_COMMAND)) {
            notify("A sessão ainda não iniciou; tente de novo em instantes.", NotificationType.WARNING)
        }
    }

    /**
     * Entrega a referência do trecho do editor à sessão **em foco** e traz a janela à frente
     * (RF-49).
     *
     * Existe porque o `Ctrl+Alt+K` do plugin oficial não alcança esta janela: as duas saídas dele
     * — `focusClaudeInTerminal` e `openClaudeInTerminal` — estão presas ao literal `"Terminal"`,
     * e as nossas sessões não estão no `ContentManager` daquela tool window (DEF-08). O que ele
     * envia por MCP chega, mas em **broadcast**: com a aba dividida, todas as panes recebem, e
     * nenhuma é o destino (Q-02).
     *
     * Aqui a entrega é dirigida, pelo mesmo `sendInput` que o `/export` usa desde D-16 — o texto
     * é digitado no PTY daquela pane e de mais nenhuma. Sem tocar no protocolo privado, sem
     * violar D-01.
     */
    fun sendEditorReference(reference: String) {
        val widget = selectedWidget()
            ?: return notify("Nenhuma sessão aberta para receber a seleção.", NotificationType.WARNING)

        if (!ClaudeTerminalSessionFactory.sendInput(widget, reference)) {
            return notify("A sessão ainda não iniciou; tente de novo em instantes.", NotificationType.WARNING)
        }

        // Sem isto a menção chegaria numa janela que o usuário não está vendo. O foco cai na pane
        // certa porque `preferredFocusableComponent` da aba já aponta para ela (RF-42).
        findToolWindow()?.activate(null)
    }

    /**
     * Envia `/usage` à sessão em foco (RF-51).
     *
     * **O resultado aparece na própria sessão, e não em popup — é o desenho, não uma limitação
     * contornável (D-43).** O `/usage` não imprime texto: desenha uma tela de TUI. Não há
     * subcomando `claude usage` para chamar em modo `-p`, o `stats-cache.json` local guarda
     * atividade e não limite de plano, e o número real vem de `/api/oauth/usage` autenticado com
     * o token do usuário — que o plugin não lê (RNF-34). Raspar o buffer para montar um popup
     * traria o DEF-01 de volta, agora sobre conteúdo que muda a cada repintura.
     *
     * Por não esperar resposta, esta é a mais simples das três saídas por PTY: não há arquivo a
     * sondar como em [copySelectedSession], nem prazo a estourar.
     */
    fun openUsage() {
        val widget = selectedWidget()
            ?: return notify("Nenhuma sessão aberta para consultar o uso.", NotificationType.WARNING)

        if (!ClaudeTerminalSessionFactory.sendInput(widget, USAGE_COMMAND)) {
            return notify("A sessão ainda não iniciou; tente de novo em instantes.", NotificationType.WARNING)
        }

        // O usuário precisa ver a tela que acabou de pedir.
        findToolWindow()?.activate(null)
    }

    private fun selectedWidget(): TerminalWidget? =
        findToolWindow()?.contentManager?.selectedContent?.getUserData(SESSION_WIDGET)

    private fun findToolWindow(): ToolWindow? =
        ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)

    /**
     * Próximo nome automático livre (RF-16).
     *
     * Lê o **nome base**, e não o `displayName`. Com uma aba mostrando `"Claude (encerrado)"`, o
     * literal `"Claude"` não estaria no conjunto e a próxima aba nasceria com nome idêntico ao de
     * uma que está na tela — defeito anterior à v1.12, corrigido de passagem porque esta linha
     * mudava de qualquer forma (T-1.70).
     *
     * [exclude] é a aba que está sendo renomeada: sem tirá-la do conjunto, apagar o nome dela
     * devolveria `"Claude (2)"` em vez de `"Claude"` (RF-62).
     */
    private fun nextTabTitle(contentManager: ContentManager, exclude: Content? = null): String =
        ClaudeTabTitle.next(
            contentManager.contents
                .filter { it !== exclude }
                .map { baseTitleOf(it) }
                .toSet(),
        )

    private fun notifyExecutableMissing(executable: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(
                "Claude Code não encontrado",
                "Não foi possível localizar o executável \"$executable\". " +
                    "Configure o caminho completo nas configurações do plugin.",
                NotificationType.WARNING,
            )
            .addAction(object : com.intellij.openapi.actionSystem.AnAction("Abrir Configurações") {
                override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                    ShowSettingsUtil.getInstance()
                        .showSettingsDialog(project, ClaudeDockConfigurable::class.java)
                }
            })
            .notify(project)
    }

    /** `internal` para o popup de seleção reusar o mesmo grupo, em vez de duplicar a chamada. */
    internal fun notify(message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification("Claude Code Dock", message, type)
            .notify(project)
    }

    companion object {
        const val TOOL_WINDOW_ID = "Claude Code Dock"
        const val NOTIFICATION_GROUP = "ClaudeCodeDock"

        /** Slash command do CLI, seguido de CR — o mesmo que digitar e pressionar Enter. */
        private const val EXPORT_COMMAND = "/export\r"

        /** _(v1.10, RF-51)_ Mesma convenção do [EXPORT_COMMAND]: CR, e não LF. */
        internal const val USAGE_COMMAND = "/usage\r"

        /**
         * Liga a aba à sessão em foco, para as ações que operam sobre a sessão selecionada.
         *
         * _(v1.7)_ Com o split, uma aba tem várias sessões: a chave passou a significar "a
         * última com foco" e é reescrita por [sessionFocused].
         */
        private val SESSION_WIDGET = Key.create<TerminalWidget>("ClaudeDockSessionWidget")

        /** Disposable da pane, pendurado no próprio componente (D-18). */
        private const val PANE_DISPOSABLE = "ClaudeDockPaneDisposable"

        /** Sessão da pane, pendurada no mesmo lugar — permite achar quem sobreviveu (RF-41). */
        private const val PANE_WIDGET = "ClaudeDockPaneWidget"

        /** Título base da aba, sem o sufixo de encerrada — evita "(encerrado) (encerrado)". */
        private val TAB_TITLE = Key.create<String>("ClaudeDockTabTitle")

        /**
         * _(v1.12)_ Se a aba já ficou sem sessão viva (RF-44).
         *
         * Guardar o **estado** em vez de inferi-lo do `displayName` é o que permite ao rename
         * recompor o rótulo em qualquer ordem (Achado 43).
         *
         * `internal` porque no headless nenhuma sessão chega a ter PTY, então o callback de
         * término nunca dispara: sem alcançar a chave, a ordem "encerra e depois renomeia" ficaria
         * sem teste, e ela é metade do defeito (T-1.71).
         */
        internal val TAB_ENDED = Key.create<Boolean>("ClaudeDockTabEnded")

        /** _(v1.12)_ Mesmo rótulo nos dois caminhos do rename — in-place e diálogo (D-55). */
        internal const val RENAME_TAB_LABEL = "Novo nome da aba:"

        /**
         * Nome base da aba, sem o sufixo de estado (RF-59).
         *
         * `internal` porque a ação de rename in-place precisa dele para preencher o campo de
         * edição: quem edita edita o **nome**, não o estado. Sem isso, renomear uma aba encerrada
         * abriria o campo com "(encerrado)" dentro, e confirmar sem mexer gravaria o sufixo como
         * parte do nome.
         */
        /**
         * Faz a aba apontar para [widget]: é a sessão que as ações do cabeçalho vão atingir
         * (RF-38, DEF-12).
         *
         * **As duas atribuições andam juntas, e é isso que o DEF-12 mostrou.** `SESSION_WIDGET`
         * responde "sobre qual sessão agir"; `preferredFocusableComponent` responde "para onde o
         * foco volta quando a janela o reassume". Enquanto só a primeira acompanhava o foco, a
         * segunda ficava presa à pane original desde a criação da aba — e qualquer item de **menu
         * popup** devolvia o foco para lá ao abrir, reescrevendo `SESSION_WIDGET` de volta para a
         * pane 1 antes de a ação rodar. Botões simples do cabeçalho (como "Uso", T-3.66) não
         * passavam por isso, e foi por isso que o defeito atravessou v1.7 a v1.11 sem aparecer.
         */
        internal fun pointTabAt(content: Content, widget: TerminalWidget) {
            content.putUserData(SESSION_WIDGET, widget)
            content.preferredFocusableComponent = widget.component
        }

        /** A sessão para a qual [content] aponta — `internal` para o teste do invariante acima. */
        internal fun selectedWidgetOf(content: Content): TerminalWidget? =
            content.getUserData(SESSION_WIDGET)

        internal fun baseTitleOf(content: Content): String =
            content.getUserData(TAB_TITLE)
                ?: content.displayName?.let { ClaudeTabTitle.display(it, ended = false) }
                ?: ClaudeTabTitle.BASE

        private val LOG = Logger.getInstance(ClaudeDockSessions::class.java)

        fun getInstance(project: Project): ClaudeDockSessions = project.service()
    }
}
