package dev.reginaldomorais.claudedock

import com.intellij.openapi.ui.Splitter
import com.intellij.ui.OnePixelSplitter
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JPanel

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
