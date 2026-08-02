package dev.reginaldomorais.claudedock

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBLabel
import com.intellij.util.IconUtil
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagLayout
import java.awt.Rectangle
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.JPanel
import javax.swing.Timer

/**
 * Cobre a sessão enquanto o Claude Code sobe.
 *
 * A sessão é iniciada digitando o comando no shell, então os primeiros instantes mostram o
 * prompt e o eco de `claude` antes da UI do CLI aparecer.
 *
 * **A capa fica por cima, e o terminal nunca é escondido.** Duas tentativas anteriores falharam
 * por motivos opostos, e as duas valem como aviso:
 *
 * - o véu do `JBLoadingPanel` é translúcido, e deixava o eco legível por baixo;
 * - esconder o terminal (`CardLayout`, `isVisible = false`) tirava dele a dimensão real, então
 *   o CLI desenhava para um tamanho inventado e redesenhava tudo ao aparecer — o rodapé ficava
 *   quebrado por um segundo.
 *
 * Sobreposto, o terminal recebe o tamanho verdadeiro desde o início e renderiza uma única vez.
 */
object ClaudeSessionLoading {

    /**
     * Quanto tempo a capa fica no ar, em milissegundos.
     *
     * Ajuste de bancada: depende de quão rápido o CLI sobe nesta máquina — hooks de sessão
     * pesados pedem mais. Curto demais deixa escapar o eco do comando; longo demais faz a aba
     * parecer lenta. Zero desliga.
     */
    private const val VISIBLE_MS = 3_000

    /** A logo, ampliada — é SVG, então cresce sem borrar. */
    private const val ICON_SCALE = 9.0f

    /** Tipadas como `Any` de propósito: ver a nota sobre overloads em [wrap]. */
    private val BOTTOM_LAYER: Any = JLayeredPane.DEFAULT_LAYER
    private val TOP_LAYER: Any = JLayeredPane.PALETTE_LAYER

    /**
     * Devolve o componente que vai para a aba: [content] com a capa sobreposta.
     *
     * A capa some depois de [visibleMs]. Prazo, e não detecção: saber que "o CLI já pintou"
     * exigiria vigiar o buffer, cuja primeira mudança é justamente o eco que queremos esconder.
     *
     * Tudo é filho de [parent], então fechar a aba leva timer, animação e capa junto.
     */
    fun wrap(
        content: JComponent,
        parent: Disposable,
        text: String,
        visibleMs: Int = VISIBLE_MS,
    ): JComponent {
        // Prazo zero desliga o recurso: sem capa, sem timer, sem camada extra.
        if (visibleMs <= 0) return content

        val cover = createCover(parent, text)
        val layers = Layers(content)
        // PALETTE_LAYER fica acima de DEFAULT_LAYER: a capa esconde, sem tirar o terminal do ar.
        // As constantes são `Integer`, e passá-las direto faz o Kotlin escolher
        // `add(Component, int index)` — o overload de índice, que ignoraria a camada.
        layers.add(content, BOTTOM_LAYER)
        layers.add(cover, TOP_LAYER)

        val timer = Timer(visibleMs) { cover.isVisible = false }.apply {
            isRepeats = false
            start()
        }
        Disposer.register(parent) { timer.stop() }

        return layers
    }

    /**
     * `JLayeredPane` não tem gerenciador de layout: sem isto os filhos ficam com tamanho zero.
     * Ambos ocupam a área inteira, um sobre o outro.
     */
    private class Layers(private val content: JComponent) : JLayeredPane() {

        override fun doLayout() {
            val area = Rectangle(0, 0, width, height)
            components.forEach { it.bounds = area }
        }

        override fun getPreferredSize(): Dimension = content.preferredSize
    }

    /** Ícone, animação e texto empilhados no centro. */
    private fun createCover(parent: Disposable, text: String): JComponent {
        val spinner = AsyncProcessIcon("ClaudeDockLoading")
        // O ícone animado é Disposable: sem isto a animação sobrevive à aba.
        Disposer.register(parent, spinner)

        // Box já empilha na vertical; redefinir o layout dela lança AWTError.
        val column = Box.createVerticalBox().apply {
            add(centered(JBLabel(IconUtil.scale(CLAUDE_ICON, null, ICON_SCALE))))
            add(Box.createVerticalStrut(JBUI.scale(24)))
            add(centered(spinner))
            add(Box.createVerticalStrut(JBUI.scale(8)))
            add(centered(JBLabel(text).apply { foreground = UIUtil.getContextHelpForeground() }))
        }

        // GridBagLayout sem restrições centra o único filho nos dois eixos.
        // Opaco: é o que esconde o terminal por baixo.
        return JPanel(GridBagLayout()).apply {
            isOpaque = true
            add(column)
        }
    }

    private fun centered(component: JComponent): JComponent = component.apply {
        alignmentX = Component.CENTER_ALIGNMENT
    }

    private val CLAUDE_ICON =
        IconLoader.getIcon("/icons/claudeDuck.svg", ClaudeSessionLoading::class.java)
}
