package dev.reginaldomorais.claudedock

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings
import dev.reginaldomorais.claudedock.settings.PiperVoices
import dev.reginaldomorais.claudedock.settings.TtsEngine
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * T-1.22-24 e T-1.67+: núcleo de TTS — montagem de comando, validação e o caminho de processo.
 *
 * Os testes exercitam [ClaudeTtsPlayback.stream], e **não** `speak`: `stream` é a metade sem áudio,
 * e é isso que permite medir o caminho de processo inteiro num ambiente headless, onde não existe
 * mixer para abrir uma `SourceDataLine`.
 */
class ClaudeTtsPlaybackTest : BasePlatformTestCase() {

    // -- validação --------------------------------------------------------------------------

    fun `test canSynthesize do piper exige executavel e modelo`() {
        val model = tempFile("model", ".onnx")

        // Sem o executável no PATH e sem fallback, recusa
        assertFalse(canPiper("piper-probe-inexistente", model.path))
        // Modelo inexistente, mesmo com executável válido, recusa
        assertFalse(canPiper("sh", "/tmp/inexistente-xyz.onnx"))
        // Campos em branco recusam
        assertFalse(canPiper("", ""))
        assertFalse(canPiper("  ", "  "))

        assertTrue(canPiper("sh", model.path))
    }

    /**
     * T-1.68: o Kokoro precisa de três arquivos — falta de qualquer um recusa.
     *
     * A voz não entra: ela sempre cai num padrão válido, e conferir se existe no arquivo custaria
     * abrir o ZIP a cada `update()` de menu.
     */
    fun `test canSynthesize do kokoro exige interpretador modelo e vozes`() {
        val model = tempFile("kokoro", ".onnx")
        val voices = tempFile("voices", ".bin")

        assertTrue(canKokoro("sh", model.path, voices.path, "pf_dora"))

        assertFalse(canKokoro("python-inexistente-xyz", model.path, voices.path, "pf_dora"))
        assertFalse(canKokoro("sh", "/tmp/nao-existe.onnx", voices.path, "pf_dora"))
        assertFalse(canKokoro("sh", model.path, "/tmp/nao-existe.bin", "pf_dora"))
    }

    // -- montagem de comando ----------------------------------------------------------------

    /** T-1.50: em 100% a flag não vai — o config.json do modelo manda (D-40). */
    fun `test piperParameters em 100 por cento omite length-scale`() {
        val args = ClaudeTtsPlayback.piperParameters("/m.onnx", 100)

        assertEquals(listOf("-m", "/m.onnx", "--output-raw"), args)
    }

    /** T-1.51: length-scale é o inverso da velocidade. */
    fun `test piperParameters converte velocidade em length-scale`() {
        assertEquals(
            listOf("-m", "/m.onnx", "--output-raw", "--length-scale", "0.500"),
            ClaudeTtsPlayback.piperParameters("/m.onnx", 200),
        )
        assertEquals(
            listOf("-m", "/m.onnx", "--output-raw", "--length-scale", "2.000"),
            ClaudeTtsPlayback.piperParameters("/m.onnx", 50),
        )
    }

    /**
     * T-1.52 (RNF-31): o argparse do piper é `type=float` e recusa vírgula decimal.
     *
     * Numa JVM pt-BR, `"%.3f".format(...)` produziria "0,667" e a síntese morreria em silêncio.
     */
    fun `test piperParameters ignora o locale da JVM`() {
        withLocale("pt-BR") {
            assertEquals(
                listOf("-m", "/m.onnx", "--output-raw", "--length-scale", "0.667"),
                ClaudeTtsPlayback.piperParameters("/m.onnx", 150),
            )
        }
    }

    /**
     * T-1.69 (RNF-31 no motor novo): o `float()` do Python recusa vírgula decimal exatamente como
     * o argparse do piper. É o mesmo bug, e ele não pode voltar pela porta do Kokoro.
     */
    fun `test kokoroParameters ignora o locale da JVM`() {
        withLocale("pt-BR") {
            val args = ClaudeTtsPlayback.kokoroParameters("/k.onnx", "/v.bin", "pf_dora", 150)

            assertEquals("1.50", args[5])
            assertFalse("A vírgula decimal mataria a síntese: ${args[5]}", args[5].contains(","))
        }
    }

    /** T-1.70: o script vai no `-c` e o resto é posicional, na ordem que o script espera. */
    fun `test kokoroParameters monta script e argumentos posicionais`() {
        val args = ClaudeTtsPlayback.kokoroParameters("/k.onnx", "/v.bin", "pm_alex", 100)

        assertEquals("-c", args[0])
        assertTrue("O script precisa ir inteiro no -c", args[1].contains("from kokoro_onnx import Kokoro"))
        assertEquals(listOf("/k.onnx", "/v.bin", "pm_alex", "1.00", "pt-br"), args.drop(2))
    }

    /**
     * T-1.71: o idioma acompanha a voz escolhida, e não uma configuração à parte — é o prefixo do
     * nome que diz em que língua a voz fala.
     */
    fun `test kokoroParameters deriva o idioma da voz`() {
        assertEquals("en-us", ClaudeTtsPlayback.kokoroParameters("/k", "/v", "af_heart", 100).last())
        assertEquals("es", ClaudeTtsPlayback.kokoroParameters("/k", "/v", "ef_dora", 100).last())
    }

    /** T-1.72: cada motor traz a própria taxa. 24000 do Kokoro, e a do Piper vem da voz. */
    fun `test buildCommand carrega a taxa de cada motor`() {
        val kokoro = ClaudeTtsPlayback.buildCommand(kokoroSettings("sh", "/k.onnx", "/v.bin", "pf_dora"))
        assertEquals(ClaudeTtsPlayback.KOKORO_SAMPLE_RATE, kokoro.sampleRate)

        val piper = ClaudeTtsPlayback.buildCommand(piperSettings("piper", "/m.onnx"))
        assertEquals(PiperVoices.DEFAULT_SAMPLE_RATE, piper.sampleRate)
    }

    /** T-1.73: guarda de regressão — o motor antigo não pode mudar de comando por tabela. */
    fun `test buildCommand do piper continua igual`() {
        val command = ClaudeTtsPlayback.buildCommand(piperSettings("piper", "/m.onnx"), speedPercent = 50)

        assertEquals("piper", command.executable)
        assertEquals(listOf("-m", "/m.onnx", "--output-raw", "--length-scale", "2.000"), command.args)
    }

    // -- caminho de processo ----------------------------------------------------------------

    /**
     * T-1.74 (RF-57): **o áudio chega em pedaços.**
     *
     * É o teste que falha se alguém devolver o desenho antigo, de ler o stdout inteiro antes de
     * tocar. Afirma o efeito observável: o primeiro pedaço chega bem antes do último.
     */
    fun `test stream entrega o audio em pedacos, e nao de uma vez`() {
        val fake = fakeEngine("printf 'aaaa'; sleep 0.4; printf 'bbbb'; sleep 0.4; printf 'cccc'")

        val arrivals = mutableListOf<Long>()
        val startedAt = System.currentTimeMillis()
        val ok = ClaudeTtsPlayback.stream("oi", command(fake, timeout = 10)) { _, _ ->
            synchronized(arrivals) { arrivals.add(System.currentTimeMillis() - startedAt) }
        }

        assertTrue(ok)
        assertTrue("Chegou tudo de uma vez: $arrivals", arrivals.size >= 2)
        assertTrue(
            "O primeiro pedaço tem de chegar bem antes do último: $arrivals",
            arrivals.last() - arrivals.first() >= 300,
        )
    }

    /**
     * T-1.75 (RNF-35, herda T-1.55): o prazo até o primeiro byte é respeitado.
     *
     * O motor falso dorme 30 s sem emitir nada; sem o cão de guarda o teste travaria.
     */
    fun `test stream aborta quando nenhum audio chega no prazo`() {
        val fake = fakeEngine("exec sleep 30")

        val startedAt = System.currentTimeMillis()
        val ok = ClaudeTtsPlayback.stream("oi", command(fake, timeout = 1)) { _, _ -> }
        val elapsedMs = System.currentTimeMillis() - startedAt

        assertFalse(ok)
        assertTrue("Voltou em ${elapsedMs}ms: o prazo não foi respeitado", elapsedMs < 15_000)
    }

    /**
     * T-1.76 (RNF-35): o prazo **não** vale para a fala já em andamento.
     *
     * Com streaming o processo vive enquanto o áudio toca — um teto total mataria a fala no meio.
     * O motor falso emite um pedaço e só termina bem depois do prazo.
     */
    fun `test stream nao aborta fala que ja comecou`() {
        val fake = fakeEngine("printf 'aaaa'; sleep 3; printf 'bbbb'")

        val startedAt = System.currentTimeMillis()
        val ok = ClaudeTtsPlayback.stream("oi", command(fake, timeout = 1)) { _, _ -> }
        val elapsedMs = System.currentTimeMillis() - startedAt

        assertTrue("A fala em andamento foi morta pelo prazo", ok)
        assertTrue("Não esperou o motor terminar: ${elapsedMs}ms", elapsedMs >= 2_500)
    }

    /**
     * T-1.56 (RNF-23): `stop()` alcança o motor de uma fala em curso.
     *
     * Afirma o **efeito**: sem a morte do processo, `stream` só voltaria no fim dos 30 s do falso.
     */
    fun `test stop aborta a fala em curso`() {
        val sentinel = File.createTempFile("tts-subiu", ".flag").apply { delete(); deleteOnExit() }
        val fake = fakeEngine("printf 'aaaa'; touch '${sentinel.path}'; exec sleep 30")

        val finished = CountDownLatch(1)
        val result = AtomicBoolean(true)
        Thread {
            result.set(ClaudeTtsPlayback.stream("oi", command(fake, timeout = 20)) { _, _ -> })
            finished.countDown()
        }.start()

        assertTrue("O motor falso não chegou a subir", waitFor { sentinel.exists() })

        ClaudeTtsPlayback.stop()

        assertTrue(
            "stream não voltou: o stop não alcançou o processo",
            finished.await(15, TimeUnit.SECONDS),
        )
        assertFalse(result.get())
    }

    /**
     * T-1.77 (RNF-33): stderr volumoso não trava a fala.
     *
     * O buffer do pipe é de dezenas de KB. Sem alguém drenando o stderr, um motor que reclame
     * muito trava na escrita e a fala nunca termina — e o sintoma seria "o áudio parou no meio".
     */
    fun `test stderr volumoso nao trava a fala`() {
        val fake = fakeEngine("i=0; while [ \$i -lt 4000 ]; do echo 'ruido de diagnostico' >&2; i=\$((i+1)); done; printf 'aaaa'")

        val received = AtomicReference(0)
        val ok = ClaudeTtsPlayback.stream("oi", command(fake, timeout = 20)) { _, count ->
            received.set(received.get() + count)
        }

        assertTrue("A fala não terminou: o stderr encheu o pipe", ok)
        assertEquals(4, received.get())
    }

    /** T-1.78: motor que falha devolve `false` em vez de fingir que tocou. */
    fun `test stream devolve false quando o motor falha`() {
        val fake = fakeEngine("echo 'ModuleNotFoundError: kokoro_onnx' >&2; exit 1")

        assertFalse(ClaudeTtsPlayback.stream("oi", command(fake, timeout = 10)) { _, _ -> })
    }

    fun `test stream com texto em branco nao lanca processo`() {
        assertFalse(ClaudeTtsPlayback.stream("", command("/bin/nao-existe", timeout = 1)) { _, _ -> })
        assertFalse(ClaudeTtsPlayback.stream("   ", command("/bin/nao-existe", timeout = 1)) { _, _ -> })
    }

    fun `test estado inicial e apos stop`() {
        assertFalse(ClaudeTtsPlayback.isPlaying())
        assertFalse(ClaudeTtsPlayback.isPaused())

        ClaudeTtsPlayback.stop()  // Não deve lançar

        assertFalse(ClaudeTtsPlayback.isPlaying())
    }

    // -- apoio ------------------------------------------------------------------------------

    private fun command(executable: String, timeout: Long) = ClaudeTtsPlayback.TtsCommand(
        executable = executable,
        args = emptyList(),
        sampleRate = 22050f,
        startTimeoutSeconds = timeout,
    )

    private fun canPiper(executable: String, model: String) =
        ClaudeTtsPlayback.canSynthesize(piperSettings(executable, model))

    private fun canKokoro(python: String, model: String, voices: String, voice: String) =
        ClaudeTtsPlayback.canSynthesize(kokoroSettings(python, model, voices, voice))

    private fun piperSettings(executable: String, model: String) = ClaudeDockSettings().apply {
        ttsEngine = TtsEngine.PIPER
        piperExecutable = executable
        piperModel = model
    }

    private fun kokoroSettings(python: String, model: String, voices: String, voice: String) =
        ClaudeDockSettings().apply {
            ttsEngine = TtsEngine.KOKORO
            kokoroPython = python
            kokoroModel = model
            kokoroVoices = voices
            kokoroVoice = voice
        }

    private fun withLocale(tag: String, body: () -> Unit) {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag(tag))
            body()
        } finally {
            Locale.setDefault(original)
        }
    }

    /**
     * Motor de mentira: ignora os argumentos e faz só o que o teste pede.
     *
     * O `exec` no último comando não é detalhe: sem ele o `sh` deixa um filho segurando a ponta de
     * escrita do pipe, e matar o `sh` não desbloqueia quem lê — um cenário que os motores reais não
     * produzem, já que piper e python são processo único (e os shims do pyenv usam `exec`).
     */
    private fun fakeEngine(body: String): String =
        File.createTempFile("fake-tts", ".sh").apply {
            writeText("#!/bin/sh\n$body\n")
            setExecutable(true)
            deleteOnExit()
        }.path

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
