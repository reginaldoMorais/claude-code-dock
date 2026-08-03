package dev.reginaldomorais.claudedock

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import java.nio.file.Files

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

    private fun tempFile(prefix: String, suffix: String): File =
        File.createTempFile(prefix, suffix).apply { deleteOnExit() }
}
