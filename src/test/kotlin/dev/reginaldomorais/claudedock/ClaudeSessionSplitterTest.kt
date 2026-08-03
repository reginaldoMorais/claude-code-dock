package dev.reginaldomorais.claudedock

import com.intellij.openapi.ui.Splitter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.swing.JComponent
import javax.swing.JPanel

/** T-1.32 a T-1.37: divisão de sessões dentro da aba (RF-36, RF-39, CB-50). */
class ClaudeSessionSplitterTest {

    private fun pane() = JPanel()

    /** Força o layout de verdade: sem isto os filhos ficam todos em 0x0 e a geometria não diz nada. */
    private fun laidOut(root: JComponent): JComponent = root.apply {
        setSize(400, 200)
        doLayout()
        // O splitter só distribui espaço quando ele próprio já tem tamanho.
        (getComponent(0) as? JComponent)?.apply {
            setSize(400, 200)
            doLayout()
        }
    }

    @Test
    fun `a raiz comeca com a sessao ocupando tudo`() {
        val first = pane()
        val root = ClaudeSessionSplitter.root(first)

        assertSame(first, root.getComponent(0))
    }

    @Test
    fun `dividir troca a sessao por um splitter com as duas`() {
        val first = pane()
        val second = pane()
        val root = ClaudeSessionSplitter.root(first)

        assertTrue(ClaudeSessionSplitter.split(first, second, stacked = false))

        val splitter = root.getComponent(0) as Splitter
        assertSame(first, splitter.firstComponent)
        assertSame(second, splitter.secondComponent)
    }

    @Test
    fun `dividir a direita poe as sessoes lado a lado`() {
        val first = pane()
        val second = pane()
        val root = laidOut(ClaudeSessionSplitter.root(first))

        ClaudeSessionSplitter.split(first, second, stacked = false)
        laidOut(root)

        // Lado a lado: a segunda começa à direita da primeira, na mesma faixa vertical.
        assertTrue(
            "second.x=${second.x} first.x=${first.x}",
            second.x > first.x,
        )
        assertEqualsGeometry(first.y, second.y)
    }

    @Test
    fun `dividir abaixo empilha as sessoes`() {
        val first = pane()
        val second = pane()
        val root = laidOut(ClaudeSessionSplitter.root(first))

        ClaudeSessionSplitter.split(first, second, stacked = true)
        laidOut(root)

        // Empilhado: a segunda começa abaixo da primeira, na mesma faixa horizontal.
        assertTrue(
            "second.y=${second.y} first.y=${first.y}",
            second.y > first.y,
        )
        assertEqualsGeometry(first.x, second.x)
    }

    @Test
    fun `dividir de novo aninha dentro do lado escolhido`() {
        val first = pane()
        val second = pane()
        val third = pane()
        val root = ClaudeSessionSplitter.root(first)

        ClaudeSessionSplitter.split(first, second, stacked = false)
        // Divide a pane de baixo: o splitter externo continua sendo o filho da raiz.
        ClaudeSessionSplitter.split(second, third, stacked = true)

        val outer = root.getComponent(0) as Splitter
        assertSame(first, outer.firstComponent)

        val inner = outer.secondComponent as Splitter
        assertSame(second, inner.firstComponent)
        assertSame(third, inner.secondComponent)
    }

    /** Pane marcada, como as que `createPane` produz. */
    private fun markedPane() = JPanel().also(ClaudeSessionSplitter::markPane)

    @Test
    fun `firstPane acha a sobrevivente sob um splitter aninhado`() {
        val first = markedPane()
        val second = markedPane()
        val third = markedPane()
        ClaudeSessionSplitter.root(first)
        ClaudeSessionSplitter.split(first, second, stacked = false)
        ClaudeSessionSplitter.split(second, third, stacked = true)

        // É daqui que sai a sessão que reassume foco e chave da aba (DEF-05).
        val inner = second.parent as Splitter
        assertSame(second, ClaudeSessionSplitter.firstPane(inner))
        assertSame(second, ClaudeSessionSplitter.firstPane(second))
    }

    @Test
    fun `contar panes reflete as divisoes da aba`() {
        val first = markedPane()
        val second = markedPane()
        val third = markedPane()
        val root = ClaudeSessionSplitter.root(first)

        assertEquals(1, ClaudeSessionSplitter.countPanes(root))

        ClaudeSessionSplitter.split(first, second, stacked = false)
        assertEquals(2, ClaudeSessionSplitter.countPanes(root))

        ClaudeSessionSplitter.split(second, third, stacked = true)
        assertEquals(3, ClaudeSessionSplitter.countPanes(root))

        // Fechar tira da conta: é o que decide se a aba ainda tem sessão viva (RF-44).
        ClaudeSessionSplitter.close(third)
        assertEquals(2, ClaudeSessionSplitter.countPanes(root))
    }

    @Test
    fun `componente sem marca nao conta como pane`() {
        val unmarked = JPanel().apply { add(JPanel()) }

        assertEquals(0, ClaudeSessionSplitter.countPanes(unmarked))
        assertNull(ClaudeSessionSplitter.firstPane(unmarked))
    }

    @Test
    fun `isSplit distingue a pane sozinha da pane dividida`() {
        val first = pane()
        val second = pane()
        ClaudeSessionSplitter.root(first)

        // Sozinha na aba: nada a trocar, girar ou fechar.
        assertFalse(ClaudeSessionSplitter.isSplit(first))

        ClaudeSessionSplitter.split(first, second, stacked = false)

        assertTrue(ClaudeSessionSplitter.isSplit(first))
        assertTrue(ClaudeSessionSplitter.isSplit(second))
    }

    @Test
    fun `pane fechada deixa de estar na arvore, e e assim que o encerramento se distingue`() {
        val first = pane()
        val second = pane()
        val root = ClaudeSessionSplitter.root(first)
        ClaudeSessionSplitter.split(first, second, stacked = false)

        ClaudeSessionSplitter.close(second)

        // DEF-03: fechar deliberadamente destaca a pane **antes** de matar o processo. É esta
        // ausência da árvore que impede a aba de ser marcada como encerrada com a irmã viva.
        assertNull(ClaudeSessionSplitter.paneOf(second, root))
        assertSame(first, ClaudeSessionSplitter.paneOf(first, root))
    }

    @Test
    fun `trocar de lado inverte as duas panes`() {
        val first = pane()
        val second = pane()
        val root = ClaudeSessionSplitter.root(first)
        ClaudeSessionSplitter.split(first, second, stacked = false)

        assertTrue(ClaudeSessionSplitter.swap(first))

        val splitter = root.getComponent(0) as Splitter
        assertSame(second, splitter.firstComponent)
        assertSame(first, splitter.secondComponent)
    }

    @Test
    fun `trocar duas vezes volta ao arranjo original`() {
        val first = pane()
        val second = pane()
        val root = ClaudeSessionSplitter.root(first)
        ClaudeSessionSplitter.split(first, second, stacked = false)

        ClaudeSessionSplitter.swap(first)
        ClaudeSessionSplitter.swap(first)

        val splitter = root.getComponent(0) as Splitter
        assertSame(first, splitter.firstComponent)
        assertSame(second, splitter.secondComponent)
    }

    @Test
    fun `girar alterna entre lado a lado e empilhado`() {
        val first = pane()
        val second = pane()
        val root = laidOut(ClaudeSessionSplitter.root(first))
        ClaudeSessionSplitter.split(first, second, stacked = false)
        laidOut(root)

        // Antes de girar: lado a lado.
        assertTrue("second.x=${second.x}", second.x > first.x)

        assertTrue(ClaudeSessionSplitter.rotate(first))
        laidOut(root)

        // Depois de girar: empilhado, e na mesma faixa horizontal.
        assertTrue("second.y=${second.y} first.y=${first.y}", second.y > first.y)
        assertEqualsGeometry(first.x, second.x)
    }

    @Test
    fun `trocar e girar sem divisao devolvem falso, sem tocar na arvore`() {
        val only = pane()
        val root = ClaudeSessionSplitter.root(only)

        assertFalse(ClaudeSessionSplitter.swap(only))
        assertFalse(ClaudeSessionSplitter.rotate(only))
        assertSame(only, root.getComponent(0))
    }

    @Test
    fun `fechar uma pane faz a irma ocupar o lugar do splitter`() {
        val first = pane()
        val second = pane()
        val root = ClaudeSessionSplitter.root(first)
        ClaudeSessionSplitter.split(first, second, stacked = false)

        // Devolve a irmã, e não só "deu certo": é dela que sai o foco depois do fechamento.
        assertSame(first, ClaudeSessionSplitter.close(second))

        // O splitter sumiu: a sobrevivente voltou a ser filha direta da raiz.
        assertSame(first, root.getComponent(0))
    }

    @Test
    fun `fechar a pane de dentro colapsa so o splitter interno`() {
        val first = pane()
        val second = pane()
        val third = pane()
        val root = ClaudeSessionSplitter.root(first)
        ClaudeSessionSplitter.split(first, second, stacked = false)
        ClaudeSessionSplitter.split(second, third, stacked = true)

        assertSame(second, ClaudeSessionSplitter.close(third))

        val outer = root.getComponent(0) as Splitter
        assertSame(first, outer.firstComponent)
        assertSame(second, outer.secondComponent)
    }

    @Test
    fun `fechar a unica pane devolve nulo, para a aba inteira ser fechada`() {
        val only = pane()
        ClaudeSessionSplitter.root(only)

        // Sem splitter em volta não há divisão a colapsar: quem decide é a camada de cima.
        assertNull(ClaudeSessionSplitter.close(only))
    }

    @Test
    fun `fechar em cadeia devolve a arvore ao estado de uma sessao so`() {
        val first = pane()
        val second = pane()
        val third = pane()
        val root = ClaudeSessionSplitter.root(first)
        ClaudeSessionSplitter.split(first, second, stacked = false)
        ClaudeSessionSplitter.split(second, third, stacked = true)

        ClaudeSessionSplitter.close(third)
        ClaudeSessionSplitter.close(second)

        // Nenhum splitter sobrou pendurado na árvore depois de desfazer as duas divisões.
        assertSame(first, root.getComponent(0))
        assertNull(ClaudeSessionSplitter.close(first))
    }

    @Test
    fun `paneOf sobe do terminal ate o bloco divisivel`() {
        val terminal = JPanel()
        val pane = JPanel().apply { add(terminal) }
        val root = ClaudeSessionSplitter.root(pane)

        assertSame(pane, ClaudeSessionSplitter.paneOf(terminal, root))
    }

    @Test
    fun `paneOf devolve nulo para componente fora da aba`() {
        val orphan = JPanel()
        val root = ClaudeSessionSplitter.root(pane())

        assertNull(ClaudeSessionSplitter.paneOf(orphan, root))
    }

    @Test
    fun `dividir componente sem pai nao explode`() {
        // Aba fechada entre o clique e a divisão: devolve falso em vez de lançar (CB-50).
        assertFalse(ClaudeSessionSplitter.split(pane(), pane(), stacked = false))
    }

    private fun assertEqualsGeometry(expected: Int, actual: Int) =
        assertTrue("esperado $expected, obtido $actual", expected == actual)
}
