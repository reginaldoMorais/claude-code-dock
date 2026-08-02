package dev.reginaldomorais.claudedock

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.JPanel

/** T-1.20: tela de carregamento que cobre a partida da sessão. */
class ClaudeSessionLoadingTest : BasePlatformTestCase() {

    fun `test o terminal continua na arvore de componentes`() {
        val terminal = JPanel()

        val wrapped = wrap(terminal)

        assertTrue(
            "o terminal precisa continuar sob a capa",
            generateSequence(terminal.parent) { it.parent }.any { it === wrapped },
        )
    }

    /**
     * O terminal **não** pode ser escondido: sem estar visível ele não recebe o tamanho real da
     * aba, e o CLI desenha para uma medida inventada — foi o que quebrou o rodapé por um segundo.
     */
    fun `test o terminal permanece visivel sob a capa`() {
        val terminal = JPanel()

        wrap(terminal)

        assertTrue("esconder o terminal falseia o tamanho da sessão", terminal.isVisible)
    }

    /** A capa é opaca e fica acima: é ela que esconde, sem tirar o terminal do ar. */
    fun `test a capa fica acima do terminal`() {
        val terminal = JPanel()
        val layers = wrap(terminal) as JLayeredPane

        val cover = layers.components.first { it !== terminal } as JComponent

        assertTrue("a capa precisa ser opaca para esconder", cover.isOpaque)
        assertTrue(
            "a capa precisa estar numa camada superior",
            layers.getLayer(cover) > layers.getLayer(terminal),
        )
    }

    /** Sem layout próprio, os filhos de um JLayeredPane nasceriam com tamanho zero. */
    fun `test as camadas ocupam a area inteira`() {
        val terminal = JPanel()
        val layers = wrap(terminal)

        layers.size = Dimension(800, 600)
        layers.doLayout()

        layers.components.forEach {
            assertEquals("cada camada ocupa a aba toda", Dimension(800, 600), it.size)
        }
    }

    /** Prazo zero desliga o recurso: a aba recebe o terminal direto, sem camada extra. */
    fun `test prazo zero devolve o proprio terminal`() {
        val terminal = JPanel()

        assertSame(terminal, wrap(terminal, millis = 0))
    }

    private fun wrap(terminal: JPanel, millis: Int = 1_500): JComponent {
        val parent = Disposer.newDisposable()
        Disposer.register(testRootDisposable, parent)

        return ClaudeSessionLoading.wrap(terminal, parent, "Iniciando…", millis)
    }
}
