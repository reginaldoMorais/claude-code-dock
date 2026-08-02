package dev.reginaldomorais.claudedock

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Os ícones são referenciados por caminho de string — dois no `plugin-terminal.xml` e um no
 * [ClaudeSessionLoading] —, e nenhum deles é verificado pelo compilador. Renomear um arquivo
 * quebra a barra de ferramentas só em tempo de execução.
 *
 * As asserções olham o arquivo, e não o ícone carregado: o tamanho que o carregador reporta
 * depende do ambiente (uma versão anterior deste teste passava aqui e falhava no CI), enquanto
 * o que precisa ser garantido é o que o SVG declara.
 */
class ClaudeIconTest : BasePlatformTestCase() {

    fun `test barra de ferramentas usa a versao monocromatica`() {
        val declarados = ICON_ATTRIBUTE.findAll(read("/META-INF/plugin-terminal.xml"))
            .map { it.groupValues[1] }
            .toList()

        // Controle: se a regex parar de casar, o forEach abaixo passaria sem verificar nada.
        assertEquals(listOf(MONO, MONO), declarados)
        declarados.forEach { assertNotNull("Ícone ausente: $it", javaClass.getResource(it)) }
    }

    /**
     * Exportado do Inkscape, o SVG vinha com dimensão em `mm`, que o carregador do IDE descarta.
     * Sem unidade é o que faz o ícone nascer no tamanho pedido.
     */
    fun `test icones declaram tamanho em pixel`() {
        assertDimensao(MONO, "20")
        assertDimensao(MONO_DARK, "20")
        // A capa multiplica este valor por ICON_SCALE.
        assertDimensao(COLORIDO, "16")
    }

    /** A plataforma resolve o sufixo `_dark` sozinha — ninguém referencia este arquivo. */
    fun `test variante escura acompanha a geometria da clara`() {
        assertNotNull("Variante escura ausente", javaClass.getResource(MONO_DARK))
        assertEquals(atributo(MONO, "viewBox"), atributo(MONO_DARK, "viewBox"))
    }

    private fun assertDimensao(path: String, esperado: String) {
        assertEquals(path, esperado, atributo(path, "width"))
        assertEquals(path, esperado, atributo(path, "height"))
    }

    private fun atributo(path: String, nome: String): String? =
        Regex("""\b$nome="([^"]+)"""").find(read(path))?.groupValues?.get(1)

    private fun read(path: String): String =
        checkNotNull(javaClass.getResource(path)) { "Recurso ausente: $path" }.readText()

    private companion object {
        const val COLORIDO = "/icons/claudeDuck.svg"
        const val MONO = "/icons/claudeDuckMono.svg"
        const val MONO_DARK = "/icons/claudeDuckMono_dark.svg"
        val ICON_ATTRIBUTE = Regex("""icon="([^"]+)"""")
    }
}
