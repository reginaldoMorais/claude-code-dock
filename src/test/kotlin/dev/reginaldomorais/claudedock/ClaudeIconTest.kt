package dev.reginaldomorais.claudedock

import com.intellij.openapi.util.IconLoader
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.scale.JBUIScale

/**
 * Os ícones são referenciados por caminho de string — dois no `plugin-terminal.xml` e um no
 * [ClaudeSessionLoading] —, e nenhum deles é verificado pelo compilador. Renomear um arquivo
 * quebra a barra de ferramentas só em tempo de execução.
 */
class ClaudeIconTest : BasePlatformTestCase() {

    fun `test barra de ferramentas usa a versao monocromatica`() {
        val declarados = ICON_ATTRIBUTE.findAll(readTerminalPluginXml()).map { it.groupValues[1] }.toList()

        // Controle: se a regex parar de casar, o teste passaria sem verificar nada.
        assertEquals(listOf(MONO, MONO), declarados)
        declarados.forEach { assertNotNull("Ícone ausente: $it", javaClass.getResource(it)) }
    }

    /** A plataforma resolve o sufixo `_dark` sozinha — ninguém referencia este arquivo. */
    fun `test versao monocromatica tem variante para tema escuro`() {
        assertNotNull(javaClass.getResource(MONO_DARK))
        assertSize(20, MONO_DARK)
    }

    fun `test versao monocromatica carrega no tamanho de barra de ferramentas`() {
        assertSize(20, MONO)
    }

    /**
     * A capa multiplica este tamanho por `ICON_SCALE`: se o SVG passar a carregar com outra
     * dimensão, a logo muda de tamanho na tela sem ninguém ter mexido no código.
     */
    fun `test logo colorida da capa carrega no tamanho esperado`() {
        assertSize(16, COLORIDO)
    }

    private fun assertSize(esperado: Int, path: String) {
        val icon = IconLoader.getIcon(path, ClaudeSessionLoading::class.java)

        assertEquals(path, JBUIScale.scale(esperado), icon.iconWidth)
        assertEquals(path, JBUIScale.scale(esperado), icon.iconHeight)
    }

    private fun readTerminalPluginXml(): String =
        checkNotNull(javaClass.getResource("/META-INF/plugin-terminal.xml")).readText()

    private companion object {
        const val COLORIDO = "/icons/claudeDuck.svg"
        const val MONO = "/icons/claudeDuckMono.svg"
        const val MONO_DARK = "/icons/claudeDuckMono_dark.svg"
        val ICON_ATTRIBUTE = Regex("""icon="([^"]+)"""")
    }
}
