package dev.reginaldomorais.claudedock

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowEP
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl
import com.intellij.ui.content.ContentManager
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalEngine
import org.jetbrains.plugins.terminal.TerminalOptionsProvider

/**
 * T-2.1 a T-2.6 — testes de integração da tool window e do ciclo de vida das sessões.
 *
 * **O que este ambiente não entrega, e por que os roteiros manuais continuam necessários.**
 * Nenhuma sessão criada aqui chega a ter PTY. Foi medido, não suposto: mesmo passando
 * `deferSessionStartUntilUiShown = false` direto ao runner, o `ttyConnector` permanece `null`
 * depois de 10 s — o processo nasce quando o componente é exibido, e no headless nada é exibido.
 *
 * Então "processo vivo" (T-2.2), "processos independentes" (T-2.3) e "nenhum PTY órfão" (T-2.4)
 * são verificados aqui pelo **encadeamento de `Disposable` que os produz**, que é onde um PTY
 * órfão nasceria. A parte observável do processo continua com T-3.1 e T-3.41, no IDE real.
 */
class ClaudeDockIntegrationTest : BasePlatformTestCase() {

    /**
     * T-2.1 — a tool window está registrada e é distinta da "Terminal" nativa (RF-01).
     *
     * A verificação é sobre o extension point, e não sobre `ToolWindowManager`: no headless
     * quem responde é `ToolWindowHeadlessManagerImpl`, e ele devolve `null` para **qualquer**
     * id — inclusive "Terminal". Um teste escrito sobre ele passaria sem medir nada.
     */
    fun `test T-2_1 a tool window esta registrada e e distinta da nativa`() {
        val ours = toolWindowEp("Claude Code Dock")
        val native = toolWindowEp("Terminal")

        assertEquals(ClaudeToolWindowFactory::class.java.name, ours.factoryClass)
        assertEquals("right", ours.anchor)

        // Colidir com o id da nativa seria o pior desfecho possível: é o problema que o
        // plugin existe para resolver.
        assertFalse(
            "A tool window do plugin não pode reusar a factory da nativa",
            ours.factoryClass == native.factoryClass,
        )
    }

    /**
     * T-2.2 — a sessão nasce como widget JediTerm **já ligado ao host**.
     *
     * O listener não é detalhe: `ShellTerminalWidget.getActions()` monta "Split Right"/"Split
     * Down" a partir dele, e enquanto o widget não teve listener essas ações simplesmente não
     * apareciam (D-33). Um widget sem listener é uma sessão com quatro itens de menu mortos.
     */
    fun `test T-2_2 a sessao nasce como widget JediTerm ligado ao host`() {
        val widget = createSession()

        val jediTerm = JBTerminalWidget.asJediTermWidget(widget)
        assertNotNull("A sessão não é um widget JediTerm", jediTerm)
        assertTrue(jediTerm is ShellTerminalWidget)
        assertNotNull(
            "Sem listener as ações de split do menu de contexto não aparecem (D-33)",
            jediTerm!!.listener,
        )
    }

    /**
     * T-2.3 — duas abas produzem sessões independentes (RF-06).
     *
     * Independência aqui é de **árvore e de ciclo de vida**: componentes distintos e
     * `Disposable` distintos. É o que garante que fechar uma não alcance a outra.
     */
    fun `test T-2_3 duas abas produzem sessoes independentes`() {
        val contentManager = headlessContentManager()
        val sessions = ClaudeDockSessions.getInstance(project)

        sessions.addSession(contentManager, "echo primeira")
        sessions.addSession(contentManager, "echo segunda")

        assertEquals(2, contentManager.contentCount)
        val first = contentManager.getContent(0)!!
        val second = contentManager.getContent(1)!!

        assertNotSame(first.component, second.component)
        assertNotSame(first.disposer, second.disposer)
        assertEquals(1, ClaudeSessionSplitter.countPanes(first.component))
        assertEquals(1, ClaudeSessionSplitter.countPanes(second.component))
    }

    /**
     * T-2.4 (a) — fechar a aba dispara o `Disposable` dela (RNF-09, CB-08).
     *
     * É o primeiro elo da corrente que mata o PTY: `removeContent` → disposer da aba.
     */
    fun `test T-2_4a fechar a aba descarta o disposable dela`() {
        val contentManager = headlessContentManager()
        ClaudeDockSessions.getInstance(project).addSession(contentManager, "echo unica")

        val content = contentManager.getContent(0)!!
        val tabDisposable = content.disposer
        assertNotNull("A aba precisa ter disposer próprio para levar a sessão junto", tabDisposable)

        // Sentinela pendurada no disposer da aba: afirma a **propagação**, que é o que solta o
        // PTY. `Disposer.isDisposed` faria a pergunta direta, mas está depreciado justamente por
        // ser pouco confiável em Disposable qualquer; `CheckedDisposable` é o substituto.
        val marker = Disposer.newCheckedDisposable()
        Disposer.register(tabDisposable!!, marker)
        assertFalse(marker.isDisposed)

        contentManager.removeContent(content, true)

        assertEquals(0, contentManager.contentCount)
        assertTrue(
            "Fechar a aba não descartou o Disposable: todo PTY dela ficaria órfão",
            marker.isDisposed,
        )
    }

    /**
     * T-2.4 (b) — o segundo elo: o widget é **filho** do `Disposable` da aba (RNF-09).
     *
     * Sem esta relação o elo (a) não serviria de nada — a aba sumiria e o processo ficaria.
     */
    fun `test T-2_4b descartar o pai da sessao descarta o widget`() {
        val parent = Disposer.newDisposable("T-2.4b")
        Disposer.register(testRootDisposable, parent)

        val widget = ClaudeTerminalSessionFactory.createSession(project, parent, "echo oi", NoopHost)

        val marker = Disposer.newCheckedDisposable()
        Disposer.register(widget, marker)
        assertFalse(marker.isDisposed)

        Disposer.dispose(parent)

        assertTrue(
            "O widget não é filho do Disposable da aba: fechar a aba deixaria o PTY vivo",
            marker.isDisposed,
        )
    }

    /**
     * T-2.5 — sem o plugin de terminal não há tool window (RF-15, CB-10).
     *
     * Desabilitar um plugin empacotado dentro do próprio processo de teste não é possível, então
     * o que se verifica é o **mecanismo** que produz esse efeito: a tool window é registrada pelo
     * descritor opcional `plugin-terminal.xml`, carregado só quando
     * `org.jetbrains.plugins.terminal` está presente. Se alguém mover a declaração para o
     * `plugin.xml` principal, o plugin passa a quebrar em IDEs sem terminal — e este teste cai.
     *
     * `descriptorPath` é lido por reflexão de propósito, **sem** `runCatching`: se a plataforma
     * deixar de expor o método num upgrade, é melhor o teste explodir do que passar calado.
     */
    fun `test T-2_5 a tool window vem do descritor opcional do terminal`() {
        val descriptor = toolWindowEp("Claude Code Dock").pluginDescriptor
        val descriptorPath = descriptor.javaClass.getMethod("getDescriptorPath").invoke(descriptor)

        assertEquals(
            "A tool window precisa vir do descritor opcional, senão o plugin exige o terminal",
            "plugin-terminal.xml",
            descriptorPath,
        )
    }

    /**
     * T-2.6 — a sessão é JediTerm/CLASSIC em **qualquer** `TerminalEngine` (Achado 27, CB-11).
     *
     * Esta é a premissa que o SPEC errou por três versões: CB-26, CB-36, CB-47 e R-15 supunham
     * que o engine escolhido pelo usuário valia para as nossas abas. Não vale — o engine é
     * propriedade da tool window que cria o widget, e `startShellTerminalWidget` devolve
     * `JBTerminalWidget` sempre. Como `getText()` e o `Esc` dependem disso, é a asserção que
     * mais merece guarda de regressão.
     *
     * Vale notar que o padrão deste ambiente **já é `REWORKED`**: o caso interessante é o normal.
     */
    fun `test T-2_6 a sessao e JediTerm em qualquer engine do usuario`() {
        val options = TerminalOptionsProvider.instance
        val original = options.terminalEngine
        try {
            TerminalEngine.entries.forEach { engine ->
                options.terminalEngine = engine

                val jediTerm = JBTerminalWidget.asJediTermWidget(createSession())

                assertTrue(
                    "Com TerminalEngine.$engine a sessão deixou de ser JediTerm/CLASSIC: " +
                        "getText() e o pre-handler do Esc dependem disso (Achado 27)",
                    jediTerm is ShellTerminalWidget,
                )
            }
        } finally {
            options.terminalEngine = original
        }
    }

    // --- apoio ---

    private fun toolWindowEp(id: String): ToolWindowEP =
        ToolWindowEP.EP_NAME.extensionList.single { it.id == id }

    private fun headlessContentManager(): ContentManager =
        ToolWindowHeadlessManagerImpl.MockToolWindow(project).contentManager

    /** Cada sessão ganha um pai próprio, preso ao teste, para não vazar entre casos. */
    private fun createSession(): TerminalWidget {
        val parent: Disposable = Disposer.newDisposable("ClaudeDockIntegrationTest")
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
}
