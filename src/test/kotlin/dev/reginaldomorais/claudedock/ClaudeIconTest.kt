package dev.reginaldomorais.claudedock

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Os ícones são referenciados por caminho de string — os do `plugin-terminal.xml` e um no
 * [ClaudeSessionLoading] —, e nenhum deles é verificado pelo compilador. Renomear um arquivo
 * quebra a barra de ferramentas só em tempo de execução.
 *
 * As asserções olham o arquivo, e não o ícone carregado: o tamanho que o carregador reporta
 * depende do ambiente (uma versão anterior deste teste passava aqui e falhava no CI), enquanto
 * o que precisa ser garantido é o que o SVG declara.
 */
class ClaudeIconTest : BasePlatformTestCase() {

    fun `test tool window usa a versao monocromatica de 20 px`() {
        assertEquals(listOf(MONO), iconsOf("toolWindow"))
    }

    fun `test acoes usam a versao monocromatica de 16 px`() {
        val declarados = iconsOf("action")

        // Controle: se a regex parar de casar, o forEach abaixo passaria sem verificar nada.
        // Antes isto era a lista exata `[MONO, MONO]`, e cada ação nova derrubava o teste por
        // contagem — ruído, não defeito. A guarda de "não vazio" preserva o controle real.
        assertFalse("A regex de ícone parou de casar", declarados.isEmpty())
        assertEquals(
            "As ações só usam a versão monocromática de 16 px",
            emptyList<String>(),
            declarados.filterNot { it == ACTION },
        )
        declarados.forEach { assertNotNull("Ícone ausente: $it", javaClass.getResource(it)) }
    }

    /**
     * O menu (`BegMenuItemUI`) só desenha no gutter ícones de até 18 px de largura; acima disso o
     * ícone vai para a coluna do texto. Trava a regra, e não só o nome do arquivo.
     */
    fun `test icone de acao cabe no gutter do menu`() {
        iconsOf("action").forEach { path ->
            val largura = checkNotNull(atributo(path, "width")) { "Sem largura: $path" }.toInt()
            assertTrue("$path tem $largura px, acima do gutter do menu", largura <= ACTION_MAX_WIDTH)
        }
    }

    /**
     * Exportado do Inkscape, o SVG vinha com dimensão em `mm`, que o carregador do IDE descarta.
     * Sem unidade é o que faz o ícone nascer no tamanho pedido.
     */
    fun `test icones declaram tamanho em pixel`() {
        assertDimensao(MONO, "20")
        assertDimensao(MONO_DARK, "20")
        assertDimensao(ACTION, "16")
        assertDimensao(ACTION_DARK, "16")
        // A capa multiplica este valor por ICON_SCALE.
        assertDimensao(COLORIDO, "16")
    }

    /** A plataforma resolve o sufixo `_dark` sozinha — ninguém referencia este arquivo. */
    fun `test variante escura acompanha a geometria da clara`() {
        listOf(MONO to MONO_DARK, ACTION to ACTION_DARK).forEach { (claro, escuro) ->
            assertNotNull("Variante escura ausente: $escuro", javaClass.getResource(escuro))
            assertEquals(escuro, atributo(claro, "viewBox"), atributo(escuro, "viewBox"))
        }
    }

    /** O ícone de ação é o mesmo desenho da tool window, só em outro tamanho. */
    fun `test icone de acao tem a mesma geometria da tool window`() {
        assertEquals(atributo(MONO, "viewBox"), atributo(ACTION, "viewBox"))
    }

    /** Ícones declarados no `icon=` dos elementos [tag] do `plugin-terminal.xml`. */
    private fun iconsOf(tag: String): List<String> =
        Regex("""<$tag\b[^>]*\bicon="([^"]+)"""")
            .findAll(read("/META-INF/plugin-terminal.xml"))
            .map { it.groupValues[1] }
            .toList()

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
        const val ACTION = "/icons/claudeDuckAction.svg"
        const val ACTION_DARK = "/icons/claudeDuckAction_dark.svg"
        const val ACTION_MAX_WIDTH = 16
    }
}
