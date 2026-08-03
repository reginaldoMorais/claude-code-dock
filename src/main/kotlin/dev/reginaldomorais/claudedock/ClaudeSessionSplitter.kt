package dev.reginaldomorais.claudedock

import com.intellij.openapi.ui.Splitter
import com.intellij.ui.OnePixelSplitter
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Divide uma aba em várias sessões lado a lado (RF-36).
 *
 * O modelo é recursivo e não guarda estado próprio: **a árvore de componentes Swing é a única
 * fonte de verdade**. Cada aba tem um painel raiz com um único filho; dividir troca esse filho
 * por um [OnePixelSplitter] que passa a conter o antigo e o novo. Dividir de novo repete o
 * processo dentro de qualquer um dos lados, o que dá aninhamento arbitrário de graça — daí "duas
 * ou mais" sessões sem limite artificial.
 *
 * Não guardar mapa de panes é deliberado, pelo mesmo motivo de D-18: a referência morre junto
 * com o componente, sem código de limpeza e sem risco de apontar para pane já fechada.
 *
 * Objeto puro de Swing: não conhece `Project`, terminal nem tool window (RNF-14).
 */
object ClaudeSessionSplitter {

    /** Metade para cada lado; o usuário arrasta o divisor a partir daí. */
    private const val PROPORTION = 0.5f

    /** Marca que identifica um bloco de sessão na árvore. */
    private const val PANE_MARK = "ClaudeDockPaneMark"

    /**
     * Painel raiz da aba: um único filho, trocado por um splitter quando a sessão se divide.
     *
     * Existe para que a primeira divisão tenha onde encaixar o splitter — sem ele, o componente
     * da sessão seria filho direto do `Content` e não haveria posição a substituir.
     */
    fun root(pane: JComponent): JPanel =
        JPanel(BorderLayout()).apply { add(pane, BorderLayout.CENTER) }

    /**
     * Sobe de [component] até a pane que ocupa uma posição divisível.
     *
     * A pane é o componente cujo pai é o painel raiz ou um splitter — ou seja, o bloco inteiro
     * que será dividido, com a capa de carregamento e o respiro junto. Devolve `null` se
     * [component] não estiver sob [root], o que acontece com aba já fechada.
     */
    fun paneOf(component: Component, root: Component): JComponent? {
        // Sem esta verificação, uma pane **fechada** ainda seria encontrada: `close` desanexa o
        // splitter da árvore, mas a pane removida continua filha dele — e o laço abaixo pararia
        // no primeiro `Splitter` que achasse, sem nunca confirmar que ele leva à aba (DEF-03).
        if (!SwingUtilities.isDescendingFrom(component, root)) return null

        var current: Component = component

        while (true) {
            val parent = current.parent ?: return null
            if (parent === root || parent is Splitter) return current as? JComponent
            current = parent
        }
    }

    /**
     * Põe [incoming] ao lado de [existing], dividindo o espaço que [existing] ocupava.
     *
     * @param stacked `true` empilha um sobre o outro ("Dividir abaixo"); `false` coloca lado a
     *   lado ("Dividir à direita").
     * @return `false` quando [existing] não está montado — nada a dividir.
     */
    fun split(existing: JComponent, incoming: JComponent, stacked: Boolean): Boolean {
        val parent = existing.parent ?: return false
        // A posição precisa ser lida **antes**: pendurar `existing` no splitter o tira do pai.
        val wasFirst = parent is Splitter && parent.firstComponent === existing

        val splitter = OnePixelSplitter(stacked, PROPORTION).apply {
            firstComponent = existing
            secondComponent = incoming
        }

        place(splitter, parent, wasFirst)
        return true
    }

    /** Se [pane] faz parte de uma divisão — ou seja, se há o que trocar, girar ou fechar. */
    fun isSplit(pane: JComponent): Boolean = pane.parent is Splitter

    /**
     * Marca [component] como uma pane, para as buscas na árvore.
     *
     * A marca vive no próprio componente (`putClientProperty`), então morre junto com ele — a
     * mesma razão de D-36 para não manter estrutura paralela.
     */
    fun markPane(component: JComponent) {
        component.putClientProperty(PANE_MARK, true)
    }

    /** Primeira pane sob [component], contando o próprio — de onde sai o foco após um fechamento. */
    fun firstPane(component: JComponent): JComponent? {
        if (component.getClientProperty(PANE_MARK) == true) return component

        return component.components
            .filterIsInstance<JComponent>()
            .firstNotNullOfOrNull { firstPane(it) }
    }

    /** Quantas panes há sob [component] — é o que decide se a aba ainda tem sessão viva. */
    fun countPanes(component: JComponent): Int {
        if (component.getClientProperty(PANE_MARK) == true) return 1

        return component.components.filterIsInstance<JComponent>().sumOf { countPanes(it) }
    }

    /**
     * Troca [pane] de lado com a irmã (RF-43).
     *
     * `Splitter.swapComponents()` é da plataforma e troca as duas referências internas, sem
     * reparentar nada — o que evita a armadilha de fazer isso à mão: `setFirstComponent` remove
     * o componente que estava naquele lado, então trocar em dois passos derruba o que o primeiro
     * passo acabou de pôr.
     *
     * @return `false` quando não há divisão — nada a trocar.
     */
    fun swap(pane: JComponent): Boolean {
        val splitter = pane.parent as? Splitter ?: return false

        splitter.swapComponents()
        return true
    }

    /**
     * Gira a divisão que contém [pane]: lado a lado ↔ empilhado (RF-43).
     *
     * @return `false` quando não há divisão — nada a girar.
     */
    fun rotate(pane: JComponent): Boolean {
        val splitter = pane.parent as? Splitter ?: return false

        splitter.orientation = !splitter.orientation
        splitter.revalidate()
        splitter.repaint()
        return true
    }

    /**
     * Remove [pane], fazendo o irmão ocupar o espaço dos dois.
     *
     * @return o irmão que sobreviveu, ou `null` quando a pane é a única da aba — aí não há split
     *   a colapsar, e quem sabe o que fazer (fechar a aba) é a camada de cima. Devolver o irmão,
     *   e não apenas `true`, é o que permite dar foco a uma sessão viva depois do fechamento.
     */
    fun close(pane: JComponent): JComponent? {
        val splitter = pane.parent as? Splitter ?: return null

        val sibling = if (splitter.firstComponent === pane) {
            splitter.secondComponent
        } else {
            splitter.firstComponent
        } ?: return null

        val parent = splitter.parent ?: return null
        val wasFirst = parent is Splitter && parent.firstComponent === splitter

        // O irmão sobe para o lugar que o splitter inteiro ocupava.
        place(sibling, parent, wasFirst)
        return sibling
    }

    /** Encaixa [component] na posição que o pai já reservava, seja splitter ou painel raiz. */
    private fun place(component: JComponent, parent: java.awt.Container, wasFirst: Boolean) {
        when {
            parent is Splitter && wasFirst -> parent.firstComponent = component
            parent is Splitter -> parent.secondComponent = component
            else -> {
                parent.removeAll()
                parent.add(component, BorderLayout.CENTER)
            }
        }

        parent.revalidate()
        parent.repaint()
    }
}
