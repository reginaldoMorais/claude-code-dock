package dev.reginaldomorais.claudedock

import com.intellij.util.SystemProperties
import org.junit.Assert.assertEquals
import org.junit.Test

/** T-1.3 e T-1.4: resolução do diretório de trabalho. */
class ClaudeWorkingDirectoryTest {

    @Test
    fun `usa a raiz do projeto quando disponivel`() {
        assertEquals("/home/user/proj", ClaudeWorkingDirectory.resolve("/home/user/proj"))
    }

    @Test
    fun `cai no home quando o projeto nao tem basePath`() {
        assertEquals(SystemProperties.getUserHome(), ClaudeWorkingDirectory.resolve(null))
    }

    @Test
    fun `cai no home quando o basePath e vazio ou em branco`() {
        assertEquals(SystemProperties.getUserHome(), ClaudeWorkingDirectory.resolve(""))
        assertEquals(SystemProperties.getUserHome(), ClaudeWorkingDirectory.resolve("   "))
    }
}
