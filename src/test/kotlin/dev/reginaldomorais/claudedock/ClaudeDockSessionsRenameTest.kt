package dev.reginaldomorais.claudedock

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import javax.swing.JPanel

/**
 * T-1.69 a T-1.71 e T-1.75 — o nome da aba (RF-58, RF-59, RF-62).
 *
 * O que se mede aqui é a **composição nome + estado**, que é onde o rename ingênuo quebra: o
 * rótulo da aba nunca foi o nome dela (Achado 43). Que o campo de edição abra sobre o rótulo é
 * T-3.80/T-3.82 — a plataforma faz essa parte, e ela não é alcançável no headless.
 */
class ClaudeDockSessionsRenameTest : BasePlatformTestCase() {

    /**
     * T-1.71, primeira ordem — encerra e **depois** renomeia: o sufixo sobrevive.
     *
     * Sem a composição num ponto só, o rename apagaria o "(encerrado)" e a aba passaria a dizer
     * que está viva com o processo morto.
     */
    fun `test T-1_71 renomear aba encerrada preserva o sufixo`() {
        val content = addContent("Claude")
        content.putUserData(ClaudeDockSessions.TAB_ENDED, true)

        ClaudeDockSessions.getInstance(project).applyTabName(content, "backend")

        assertEquals("backend (encerrado)", content.displayName)
        assertEquals("backend", ClaudeDockSessions.baseTitleOf(content))
    }

    /**
     * T-1.71, segunda ordem — renomeia e **depois** encerra: o nome sobrevive.
     *
     * Recompor a partir do nome base é o que impede o `displayName` de voltar ao automático.
     */
    fun `test T-1_71 encerrar depois do rename preserva o nome`() {
        val content = addContent("Claude")
        val sessions = ClaudeDockSessions.getInstance(project)

        sessions.applyTabName(content, "backend")
        assertEquals("backend", content.displayName)

        // O que `markEndedWhenLast` faz quando a última sessão da aba morre.
        content.putUserData(ClaudeDockSessions.TAB_ENDED, true)
        sessions.applyTabName(content, ClaudeDockSessions.baseTitleOf(content))

        assertEquals("backend (encerrado)", content.displayName)
    }

    /** T-1.71 — aba viva não ganha sufixo inventado. */
    fun `test T-1_71 aba viva nao ganha sufixo`() {
        val content = addContent("Claude")

        ClaudeDockSessions.getInstance(project).applyTabName(content, "backend")

        assertEquals("backend", content.displayName)
    }

    /**
     * T-1.69 — nome em branco significa "volte ao padrão", e não erro (RF-62).
     *
     * A aba renomeada precisa sair do conjunto de nomes usados antes de escolher o automático:
     * sem isso, apagar o nome devolveria "Claude (2)" com uma aba só na janela.
     */
    fun `test T-1_69 nome em branco devolve o automatico`() {
        val content = addContent("Claude")
        val sessions = ClaudeDockSessions.getInstance(project)

        sessions.applyTabName(content, "backend")
        assertEquals("backend", content.displayName)

        sessions.applyTabName(content, "   ")

        assertEquals("Claude", content.displayName)
    }

    /**
     * T-1.70 — o nome base ignora o sufixo, e é ele que alimenta o próximo automático.
     *
     * Defeito anterior à v1.12: `nextTabTitle` lia o `displayName`, então uma aba em
     * "Claude (encerrado)" liberava o literal "Claude" e a próxima aba nascia com nome idêntico
     * ao de uma que estava na tela.
     */
    fun `test T-1_70 o nome base alimenta o proximo automatico`() {
        val content = addContent("Claude")
        content.putUserData(ClaudeDockSessions.TAB_ENDED, true)
        ClaudeDockSessions.getInstance(project).applyTabName(content, "Claude")

        assertEquals("Claude (encerrado)", content.displayName)
        assertEquals("Claude", ClaudeDockSessions.baseTitleOf(content))
        assertEquals(
            "Claude (2)",
            ClaudeTabTitle.next(setOf(ClaudeDockSessions.baseTitleOf(content))),
        )
    }

    /** T-1.70 — aba que não criamos cai para o `displayName`, também sem o sufixo. */
    fun `test T-1_70 aba sem nome base cai para o displayName limpo`() {
        assertEquals("Claude", ClaudeDockSessions.baseTitleOf(addContent("Claude (encerrado)")))
    }

    /**
     * T-1.75 — sem aba aberta, avisa em vez de não fazer nada (Fluxo de erro M).
     *
     * No headless o `ToolWindowManager` devolve `null` para qualquer id, que é exatamente o estado
     * da janela vazia de RF-18. Pelo caminho in-place este caso não existe: sem `Content`, a base
     * da plataforma nem mostra o item.
     */
    fun `test T-1_75 renomear sem aba avisa e nao altera nada`() {
        val recebidas = collectNotifications()

        ClaudeDockSessions.getInstance(project).renameSelectedTab()

        assertEquals("Um aviso, e um só", 1, recebidas.size)
        assertTrue(
            "A mensagem deve dizer que falta aba: ${recebidas.first().content}",
            recebidas.first().content.contains("Nenhuma aba"),
        )
    }

    /** T-1.75 — o mesmo para o subtítulo da pane (RF-60). */
    fun `test T-1_75 renomear sessao sem pane avisa`() {
        val recebidas = collectNotifications()

        ClaudeDockSessions.getInstance(project).renameSelectedPane()

        assertEquals("Um aviso, e um só", 1, recebidas.size)
        assertTrue(
            "A mensagem deve dizer que falta sessão: ${recebidas.first().content}",
            recebidas.first().content.contains("Nenhuma sessão"),
        )
    }

    /**
     * DEF-12 — apontar a aba para uma sessão move **as duas** referências, sempre juntas.
     *
     * Enquanto `SESSION_WIDGET` acompanhava o foco e `preferredFocusableComponent` ficava preso à
     * pane original, abrir um menu popup devolvia o foco para a pane 1 e reescrevia a primeira de
     * volta — então todo item de menu agia sobre a pane errada. Este teste fixa o invariante que o
     * conserto criou: quem aponta a aba aponta os dois campos.
     */
    fun `test DEF-12 apontar a aba move sessao e foco preferido juntos`() {
        val content = addContent("Claude")
        val primeira = createSession()
        val segunda = createSession()

        ClaudeDockSessions.pointTabAt(content, primeira)
        assertSame(primeira, ClaudeDockSessions.selectedWidgetOf(content))
        assertSame(primeira.component, content.preferredFocusableComponent)

        ClaudeDockSessions.pointTabAt(content, segunda)
        assertSame(segunda, ClaudeDockSessions.selectedWidgetOf(content))
        assertSame(
            "o foco preferido tem de seguir a sessão apontada, senão o popup o devolve à pane 1",
            segunda.component,
            content.preferredFocusableComponent,
        )
    }

    // --- apoio ---

    private fun collectNotifications(): List<Notification> {
        val recebidas = mutableListOf<Notification>()

        project.messageBus.connect(testRootDisposable)
            .subscribe(
                Notifications.TOPIC,
                object : Notifications {
                    override fun notify(notification: Notification) {
                        if (notification.groupId == ClaudeDockSessions.NOTIFICATION_GROUP) {
                            recebidas += notification
                        }
                    }
                },
            )

        return recebidas
    }

    /** Sessão real, presa ao teste — o mesmo molde do `ClaudeDockIntegrationTest`. */
    private fun createSession(): TerminalWidget {
        val parent: Disposable = Disposer.newDisposable("ClaudeDockSessionsRenameTest")
        Disposer.register(testRootDisposable, parent)

        return ClaudeTerminalSessionFactory.createSession(project, parent, "echo oi", NoopHost)
    }

    private object NoopHost : ClaudeTerminalSessionFactory.SessionHost {
        override fun openNewSession() = Unit
        override fun splitSession(widget: TerminalWidget, stacked: Boolean) = Unit
        override fun closeSession(widget: TerminalWidget) = Unit
        override fun selectSiblingTab(next: Boolean) = Unit
        override fun sessionFocused(widget: TerminalWidget) = Unit
    }

    /** Aba de mentira, ligada a um `ContentManager` real — é dele que sai o nome automático. */
    private fun addContent(title: String): Content {
        val manager: ContentManager =
            ToolWindowHeadlessManagerImpl.MockToolWindow(project).contentManager
        val content = ContentFactory.getInstance().createContent(JPanel(), title, false)

        manager.addContent(content)
        return content
    }
}
