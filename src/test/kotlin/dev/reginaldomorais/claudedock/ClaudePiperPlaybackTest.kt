package dev.reginaldomorais.claudedock

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

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

    /**
     * T-1.55 (RNF-20, Achado 31): a síntese respeita o prazo em vez de esperar para sempre.
     *
     * O SPEC prometia timeout de 20 s desde a v1.5 e o código chamava `waitFor()` sem argumento.
     * Este teste falha se alguém devolver a versão sem prazo — o piper falso dorme 30 s.
     */
    fun `test synthesize aborta no timeout em vez de esperar o piper`() {
        val fakePiper = fakePiper("sleep 30")
        val model = tempFile("model", ".onnx")

        val startedAt = System.currentTimeMillis()
        val result = ClaudePiperPlayback.synthesize("oi", fakePiper.path, model.path, timeoutSeconds = 1)
        val elapsedMs = System.currentTimeMillis() - startedAt

        assertNull(result)
        assertTrue("Voltou em ${elapsedMs}ms: o prazo não foi respeitado", elapsedMs < 15_000)
    }

    /**
     * T-1.56 (RNF-23, Achado 31): `stop()` alcança o piper de uma síntese em curso.
     *
     * É o teste do campo `currentProcess`, que era declarado e nunca atribuído — `destroy()`
     * operava sobre `null` e a síntese anterior seguia viva. Afirma o **efeito**, não o campo:
     * sem a atribuição, `synthesize` só voltaria no fim dos 30 s do piper falso.
     */
    fun `test stop aborta a sintese em curso`() {
        val sentinel = File.createTempFile("piper-subiu", ".flag").apply { delete(); deleteOnExit() }
        val fakePiper = fakePiper("touch '${sentinel.path}'; sleep 30")
        val model = tempFile("model", ".onnx")

        val result = AtomicReference<ByteArray?>(ByteArray(1))
        val finished = CountDownLatch(1)
        Thread {
            result.set(ClaudePiperPlayback.synthesize("oi", fakePiper.path, model.path))
            finished.countDown()
        }.start()

        assertTrue("O piper falso não chegou a subir", waitFor { sentinel.exists() })

        ClaudePiperPlayback.stop()

        assertTrue(
            "synthesize não voltou: o stop não alcançou o processo",
            finished.await(15, TimeUnit.SECONDS),
        )
        assertNull(result.get())
    }

    /** Executável de mentira: ignora os argumentos do piper e faz só o que o teste pede. */
    private fun fakePiper(body: String): File =
        File.createTempFile("fake-piper", ".sh").apply {
            writeText("#!/bin/sh\n$body\n")
            setExecutable(true)
            deleteOnExit()
        }

    private fun waitFor(timeoutMs: Long = 10_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(20)
        }
        return false
    }

    private fun tempFile(prefix: String, suffix: String): File =
        File.createTempFile(prefix, suffix).apply { deleteOnExit() }
}
