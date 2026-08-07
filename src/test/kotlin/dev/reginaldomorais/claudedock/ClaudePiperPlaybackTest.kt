package dev.reginaldomorais.claudedock

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import java.util.Locale

/**
 * T-1.22-24: ClaudePiperPlayback síntese e reprodução.
 *
 * Tests do núcleo de TTS — validação do executável/modelo (sem lançar o piper real em testes)
 * e playback de PCM cru.
 */
class ClaudePiperPlaybackTest : BasePlatformTestCase() {

    fun `test canSynthesize retorna true com executavel e modelo validos`() {
        val modelFile = tempFile("model", ".onnx")
        val exeName = "piper-probe"  // Inexistente propositalmente

        // Sem o executável no PATH e sem fallback, retorna false
        assertFalse(ClaudePiperPlayback.canSynthesize(exeName, modelFile.path))

        // Modelo inexistente, mesmo que executável exista
        assertFalse(ClaudePiperPlayback.canSynthesize("sh", "/tmp/inexistente-xyz.onnx"))
    }

    fun `test canSynthesize com modelo inexistente e recusado`() {
        assertFalse(ClaudePiperPlayback.canSynthesize("piper", "/nonexistent/model.onnx"))
    }

    fun `test canSynthesize com campo vazio e recusado`() {
        assertFalse(ClaudePiperPlayback.canSynthesize("", ""))
        assertFalse(ClaudePiperPlayback.canSynthesize("  ", "  "))
    }

    fun `test synthesize com texto vazio e retorna null`() {
        assertNull(ClaudePiperPlayback.synthesize("", "piper", "/tmp/model.onnx"))
        assertNull(ClaudePiperPlayback.synthesize("   ", "piper", "/tmp/model.onnx"))
    }

    fun `test playBytes com array vazio e retorna false`() {
        assertFalse(ClaudePiperPlayback.playBytes(ByteArray(0)))
    }

    fun `test state tracking de stop`() {
        // isPlaying falso após stop
        assertFalse(ClaudePiperPlayback.isPlaying())
        assertFalse(ClaudePiperPlayback.isPaused())

        ClaudePiperPlayback.stop()  // Should not throw
        assertFalse(ClaudePiperPlayback.isPlaying())
    }

    /** T-1.50: em 100% a flag não vai — o config.json do modelo manda (D-40). */
    fun `test piperParameters em 100 por cento omite length-scale`() {
        val args = ClaudePiperPlayback.piperParameters("/m.onnx", 100)

        assertEquals(listOf("-m", "/m.onnx", "--output-raw"), args)
    }

    /** T-1.51: length-scale é o inverso da velocidade. */
    fun `test piperParameters converte velocidade em length-scale`() {
        assertEquals(
            listOf("-m", "/m.onnx", "--output-raw", "--length-scale", "0.500"),
            ClaudePiperPlayback.piperParameters("/m.onnx", 200),
        )
        assertEquals(
            listOf("-m", "/m.onnx", "--output-raw", "--length-scale", "2.000"),
            ClaudePiperPlayback.piperParameters("/m.onnx", 50),
        )
    }

    /**
     * T-1.52 (RNF-31): o argparse do piper é `type=float` e recusa vírgula decimal.
     *
     * Numa JVM pt-BR, `"%.3f".format(...)` produziria "0,667" e a síntese morreria em silêncio.
     */
    fun `test piperParameters ignora o locale da JVM`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("pt-BR"))

            assertEquals(
                listOf("-m", "/m.onnx", "--output-raw", "--length-scale", "0.667"),
                ClaudePiperPlayback.piperParameters("/m.onnx", 150),
            )
        } finally {
            Locale.setDefault(original)
        }
    }

    private fun tempFile(prefix: String, suffix: String): File =
        File.createTempFile(prefix, suffix).apply { deleteOnExit() }
}
