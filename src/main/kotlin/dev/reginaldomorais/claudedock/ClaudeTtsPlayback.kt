package dev.reginaldomorais.claudedock

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.diagnostic.Logger
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings
import dev.reginaldomorais.claudedock.settings.KokoroVoices
import dev.reginaldomorais.claudedock.settings.PiperVoices
import dev.reginaldomorais.claudedock.settings.TtsEngine
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * ÚNICO ponto de acoplamento com os motores de TTS e com `javax.sound.sampled` (RNF-19).
 *
 * Responsabilidades:
 * - Validar que o motor escolhido tem tudo o que precisa
 * - Montar a linha de comando de cada motor
 * - Lançar o processo, ler PCM do stdout **em pedaços** e tocar enquanto chega (RF-57)
 * - Pausar, retomar, parar
 * - Limpar recursos de áudio (RNF-22)
 *
 * Nenhum acoplamento a Project, serviços, actions, ou UI. Lógica pura.
 *
 * **O que varia entre motores é dado, não fluxo** ([TtsCommand]). Existe um único
 * `createProcess()` no plugin, e ele é o de [stream] (RNF-32).
 */
object ClaudeTtsPlayback {

    private val LOG = Logger.getInstance(ClaudeTtsPlayback::class.java)

    private var currentLine: SourceDataLine? = null
    private var currentProcess: Process? = null

    /**
     * Último processo que **nós** matamos.
     *
     * Serve para não chamar de falha o que foi cancelamento: quem para uma fala, ou começa outra
     * por cima, mata o motor, e ele sai com código 137. Sem esta distinção o `idea.log` ganha um
     * WARN a cada `stop()` — ruído que atrapalha justamente quem for diagnosticar uma falha real.
     */
    private val abortedProcess = AtomicReference<Process?>(null)

    /**
     * Prazos até o **primeiro byte** de áudio, por motor (RNF-35).
     *
     * Não é prazo total de propósito: com reprodução em streaming o processo vive legitimamente
     * enquanto a fala toca — 47 s de áudio, mais o tempo que o usuário deixar pausado — e um teto
     * total mataria a fala no meio.
     *
     * O do Kokoro é maior porque ele paga ~1,5 s de partida do Python e da carga do modelo antes
     * de sintetizar qualquer coisa; 30 s é folga para máquina fria, não estimativa de síntese.
     */
    private const val PIPER_START_TIMEOUT_SECONDS = 20L
    private const val KOKORO_START_TIMEOUT_SECONDS = 30L

    /** Quantas linhas do stderr guardar para o log quando o processo falha (RNF-33). */
    private const val STDERR_TAIL_LINES = 5

    /** Tudo que varia entre motores, e nada além disso. */
    internal data class TtsCommand(
        val executable: String,
        val args: List<String>,
        val sampleRate: Float,
        val startTimeoutSeconds: Long,
    )

    /**
     * Verifica se a síntese é possível com o motor escolhido. Fora da EDT. Sem exceções.
     *
     * Piper precisa do executável e do modelo; Kokoro precisa do interpretador, do modelo, do
     * arquivo de vozes e de uma voz escolhida.
     */
    fun canSynthesize(settings: ClaudeDockSettings): Boolean = when (settings.ttsEngine) {
        TtsEngine.PIPER -> hasExecutable(settings.effectivePiperExecutable()) &&
            isReadableFile(settings.effectivePiperModel())

        // A voz não entra na validação: `effectiveKokoroVoice()` sempre cai num padrão válido, e
        // conferir se ela existe no arquivo custaria abrir o ZIP a cada `update()` de menu. Voz
        // inexistente falha na síntese, com a mensagem do próprio Kokoro no log (RNF-33).
        TtsEngine.KOKORO -> hasExecutable(settings.effectiveKokoroPython()) &&
            isReadableFile(settings.effectiveKokoroModel()) &&
            isReadableFile(settings.effectiveKokoroVoices())
    }

    /** Monta a linha de comando do motor escolhido. */
    internal fun buildCommand(
        settings: ClaudeDockSettings,
        speedPercent: Int = settings.effectiveSpeechSpeed(),
    ): TtsCommand = when (settings.ttsEngine) {
        TtsEngine.PIPER -> {
            val model = settings.effectivePiperModel()
            TtsCommand(
                executable = settings.effectivePiperExecutable(),
                args = piperParameters(model, speedPercent),
                // A taxa vem da voz, nunca de constante: `low`/`x_low` são 16000 Hz e só o
                // sidecar sabe disso (DEF-10).
                sampleRate = PiperVoices.sampleRate(model),
                startTimeoutSeconds = PIPER_START_TIMEOUT_SECONDS,
            )
        }

        TtsEngine.KOKORO -> TtsCommand(
            executable = settings.effectiveKokoroPython(),
            args = kokoroParameters(
                modelPath = settings.effectiveKokoroModel(),
                voicesPath = settings.effectiveKokoroVoices(),
                voice = settings.effectiveKokoroVoice(),
                speedPercent = speedPercent,
            ),
            sampleRate = KOKORO_SAMPLE_RATE,
            startTimeoutSeconds = KOKORO_START_TIMEOUT_SECONDS,
        )
    }

    /**
     * Monta os argumentos do piper (RF-47).
     *
     * `length-scale` é o inverso da velocidade: fonema mais longo, fala mais lenta.
     *
     * Em 100% a flag é omitida de propósito — o `config.json` do modelo traz o `length_scale`
     * dele, e passar 1.0 sobrescreveria esse padrão de fábrica da voz (D-40).
     *
     * `Locale.ROOT` é obrigatório: o argparse do piper é `type=float` e recusa vírgula decimal,
     * que é justamente o que uma JVM pt-BR produziria (RNF-31).
     */
    internal fun piperParameters(modelPath: String, speedPercent: Int): List<String> {
        val base = listOf("-m", modelPath, "--output-raw")
        if (speedPercent == ClaudeDockSettings.DEFAULT_SPEECH_SPEED) return base

        val lengthScale = String.format(Locale.ROOT, "%.3f", 100.0 / speedPercent)
        return base + listOf("--length-scale", lengthScale)
    }

    /**
     * Monta os argumentos do Kokoro: o script vai inteiro no `-c`, o resto é posicional.
     *
     * `Locale.ROOT` vale aqui pelo mesmo motivo do piper (RNF-31) — o `float()` do Python recusa
     * vírgula decimal exatamente como o argparse.
     *
     * A velocidade do Kokoro é direta (maior = mais rápido), o oposto do `length-scale`.
     */
    internal fun kokoroParameters(
        modelPath: String,
        voicesPath: String,
        voice: String,
        speedPercent: Int,
    ): List<String> = listOf(
        "-c",
        KOKORO_SCRIPT,
        modelPath,
        voicesPath,
        voice,
        String.format(Locale.ROOT, "%.2f", speedPercent / 100.0),
        KokoroVoices.languageOf(voice),
    )

    /**
     * Sintetiza e entrega o PCM ao [sink] **conforme ele chega**, sem acumular (RF-57).
     *
     * Não toca em áudio: é o que permite testar todo o caminho de processo em ambiente headless,
     * onde não existe mixer.
     *
     * Três detalhes que não são estilo:
     *
     * - O texto vai para o stdin **em outra thread**: o motor trava se o buffer do pipe encher sem
     *   ninguém lendo, e quem lê é o laço abaixo.
     * - O stderr é drenado **em outra thread**, e as últimas linhas vão para o log quando o
     *   processo falha. Sem isso, uma falha do Python vira só "exit code 1" (RNF-33).
     * - O prazo vale até o primeiro byte, e é implementado por um cão de guarda que mata o
     *   processo. Um `waitFor` com prazo não serviria: o processo pode viver legitimamente por
     *   todo o tempo da fala (RNF-35).
     */
    internal fun stream(
        text: String,
        command: TtsCommand,
        sink: (ByteArray, Int) -> Unit,
    ): Boolean {
        if (text.isBlank()) return false

        var launched: Process? = null
        return try {
            val process = GeneralCommandLine(command.executable)
                .withParameters(command.args)
                .withCharset(Charsets.UTF_8)
                .createProcess()

            launched = process
            currentProcess = process

            feedStdin(process, text)
            val stderr = drainStderr(process)

            val firstByte = CountDownLatch(1)
            startWatchdog(process, firstByte, command.startTimeoutSeconds)

            var total = 0L
            val buffer = ByteArray(8192)
            try {
                while (true) {
                    val read = process.inputStream.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue

                    firstByte.countDown()
                    total += read
                    sink(buffer, read)
                }
            } catch (e: IOException) {
                // Caminho normal de cancelamento e de prazo estourado: quem aborta fecha o fluxo
                // justamente para tirar esta thread do `read`.
                LOG.debug("TTS output closed while reading", e)
                return false
            }
            firstByte.countDown()  // Libera o cão de guarda quando o áudio acabou antes do prazo.

            process.waitFor()
            val exitCode = process.exitValue()
            if (exitCode != 0) {
                // Também é o caminho de uma fala cancelada por stop(): o destroy mata o motor e
                // ele sai com código diferente de zero.
                LOG.warn("TTS process failed with exit code $exitCode${stderrTail(stderr)}")
                return false
            }

            total > 0
        } catch (e: Exception) {
            LOG.warn("Failed to synthesize with ${command.executable}", e)
            false
        } finally {
            // Só limpa se ninguém tiver começado outra fala no meio-tempo.
            if (currentProcess === launched) currentProcess = null
        }
    }

    /**
     * Sintetiza e toca, **bloqueando enquanto o áudio toca**. Fora da EDT (chamador é responsável).
     *
     * O `write` da linha bloqueia quando o buffer enche, e isso é contrapressão de graça: o pipe do
     * processo enche junto e o motor para de sintetizar sozinho. É também o que faz a pausa parar a
     * síntese, e não só o som.
     */
    internal fun speak(text: String, command: TtsCommand): Boolean {
        stopCurrent()  // Uma fala por vez (RNF-23)

        val format = AudioFormat(command.sampleRate, 16, 1, true, false)  // 16-bit, mono, LE
        val line = try {
            AudioSystem.getSourceDataLine(format)
        } catch (e: Exception) {
            LOG.warn("No audio line available for ${command.sampleRate.toInt()} Hz", e)
            return false
        }

        return try {
            line.open(format)
            line.start()
            currentLine = line

            val ok = stream(text, command) { buffer, count -> line.write(buffer, 0, count) }
            if (ok) line.drain()
            ok
        } catch (e: Exception) {
            LOG.warn("Failed to play audio", e)
            false
        } finally {
            closeLine(line)
            if (currentLine === line) currentLine = null
        }
    }

    fun pause() {
        currentLine?.let { if (it.isRunning) it.stop() }
    }

    fun resume() {
        currentLine?.let { if (!it.isRunning) it.start() }
    }

    fun stop() {
        stopCurrent()
    }

    fun isPlaying(): Boolean = currentLine?.isRunning == true

    fun isPaused(): Boolean = currentLine?.isRunning == false

    private fun stopCurrent() {
        currentLine?.let { line ->
            try {
                line.stop()
                // O flush vem antes do close de propósito: ele descarta o que está na fila e
                // destrava um `write` bloqueado na thread que está tocando.
                line.flush()
            } catch (e: Exception) {
                LOG.warn("Error stopping audio line", e)
            } finally {
                closeLine(line)
                currentLine = null
            }
        }

        abort(currentProcess)
        currentProcess = null
    }

    /**
     * Encerra o processo **e fecha o stdout dele**.
     *
     * Fechar o fluxo não é zelo, é o que desbloqueia quem está lendo: matar o processo sozinho não
     * basta quando ele deixou um filho segurando a ponta de escrita do pipe — a leitura ficaria
     * parada até esse filho morrer. Medido: sem o `close`, o cancelamento esperava os 30 s do
     * motor falso.
     */
    private fun abort(process: Process?) {
        if (process == null) return

        abortedProcess.set(process)

        try {
            process.destroyForcibly()
        } catch (e: Exception) {
            LOG.warn("Error destroying TTS process", e)
        }

        try {
            process.inputStream.close()
        } catch (e: Exception) {
            LOG.debug("Error closing the TTS process output", e)
        }
    }

    private fun closeLine(line: SourceDataLine) {
        try {
            line.close()
        } catch (e: Exception) {
            LOG.warn("Error closing audio line", e)
        }
    }

    private fun feedStdin(process: Process, text: String) {
        Thread {
            try {
                process.outputStream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            } catch (e: Exception) {
                // Esperado quando a fala é cancelada: o processo morreu e o pipe fechou.
                LOG.debug("Could not write text to the TTS process", e)
            }
        }.apply { isDaemon = true; name = "claude-dock-tts-stdin" }.start()
    }

    private fun drainStderr(process: Process): ArrayDeque<String> {
        val tail = ArrayDeque<String>()
        Thread {
            try {
                process.errorStream.bufferedReader().forEachLine { line ->
                    synchronized(tail) {
                        tail.addLast(line)
                        if (tail.size > STDERR_TAIL_LINES) tail.removeFirst()
                    }
                }
            } catch (e: Exception) {
                LOG.debug("Could not read the TTS process stderr", e)
            }
        }.apply { isDaemon = true; name = "claude-dock-tts-stderr" }.start()

        return tail
    }

    private fun stderrTail(tail: ArrayDeque<String>): String {
        val lines = synchronized(tail) { tail.toList() }
        return if (lines.isEmpty()) "" else ": ${lines.joinToString(" | ")}"
    }

    private fun startWatchdog(process: Process, firstByte: CountDownLatch, timeoutSeconds: Long) {
        Thread {
            if (!firstByte.await(timeoutSeconds, TimeUnit.SECONDS)) {
                LOG.warn("TTS produced no audio within ${timeoutSeconds}s")
                process.destroyForcibly()
            }
        }.apply { isDaemon = true; name = "claude-dock-tts-watchdog" }.start()
    }

    private fun isReadableFile(path: String): Boolean {
        if (path.isBlank()) return false
        return File(path.trim()).let { it.isFile && it.canRead() }
    }

    /**
     * Resolve um executável: caminho explícito, depois PATH, depois os diretórios de sempre.
     *
     * Mesma regra para os dois motores — é a mesma pergunta, e duplicá-la só criaria duas versões
     * para divergirem. Padronizado com a busca do `claude`.
     */
    private fun hasExecutable(executable: String): Boolean {
        if (executable.isBlank()) return false
        val trimmed = executable.trim()

        if (trimmed.contains("/")) {
            val file = File(trimmed)
            if (file.isFile && file.canExecute()) return true
        }

        if (PathEnvironmentVariableUtil.findInPath(trimmed) != null) return true

        val fallbacks = listOf(
            File(System.getProperty("user.home"), ".local/bin").path,
            "/usr/local/bin",
        )
        return fallbacks.any { dir -> File(dir, trimmed).let { it.isFile && it.canExecute() } }
    }

    /** O Kokoro emite sempre nesta taxa (`kokoro_onnx/config.py: SAMPLE_RATE = 24000`). */
    internal const val KOKORO_SAMPLE_RATE = 24000f

    /**
     * Script do Kokoro, embutido e passado no `-c` (D-47).
     *
     * Mora aqui, e não em `resources`, para não existir extração para disco, cache a invalidar,
     * nem versão defasada — e para ser testável como valor puro. Se passar de ~30 linhas, vira
     * arquivo.
     *
     * Quatro pontos são medição, não estilo:
     *
     * - `intra_op_num_threads = 4`: o `kokoro-onnx` cria a sessão sem `SessionOptions`, e o default
     *   do onnxruntime mede **mais lento que tempo real** nesta máquina (RTF 1,56 contra 0,37).
     *   Fixo em 4 porque 12 mediu pior que 4 (Achado 38).
     * - O teto de palavras no primeiro pedaço: o tempo até falar depende só da primeira frase, e
     *   cortá-la leva o primeiro pedaço de 1,81 s para 0,72 s (Achado 40).
     * - O `flush()` por pedaço: sem ele o buffer do stdout do Python engole o ganho inteiro.
     * - O prefixo `" . "` só no primeiro pedaço: ele evita que a primeira palavra saia cortada, e
     *   repeti-lo em todo pedaço estica a fala (Achado 41).
     *
     * `from_session` usa `session._model_path`, atributo privado do onnxruntime — é o único jeito
     * de passar `SessionOptions`. Verificado em 1.27.0; se quebrar num upgrade, o `except`
     * sintetiza 4x mais devagar em vez de não sintetizar.
     */
    internal val KOKORO_SCRIPT = """
import sys, re
import numpy as np, onnxruntime as ort
from kokoro_onnx import Kokoro

model, voices, voice, speed, lang = sys.argv[1:6]
opts = ort.SessionOptions()
opts.intra_op_num_threads = 4
opts.inter_op_num_threads = 1
try:
    session = ort.InferenceSession(model, sess_options=opts, providers=["CPUExecutionProvider"])
    kokoro = Kokoro.from_session(session, voices)
except Exception:
    kokoro = Kokoro(model, voices)

def split_chunks(text, first_cap=40):
    # Corta na pontuacao. O primeiro pedaco tem teto de palavras: e dele que sai o primeiro som.
    out = [f.strip() for f in re.split(r"(?<=[,.;:!?\n])\s+", text) if re.search(r"\w", f)]
    if out and len(out[0]) > first_cap * 2:
        words = out[0].split()
        head = []
        while words and len(" ".join(head)) < first_cap:
            head.append(words.pop(0))
        out = [" ".join(head), " ".join(words)] + out[1:]
    return out

for index, chunk in enumerate(split_chunks(sys.stdin.buffer.read().decode("utf-8"))):
    samples, _ = kokoro.create(
        " . " + chunk if index == 0 else chunk,
        voice=voice, speed=float(speed), lang=lang,
    )
    sys.stdout.buffer.write((np.clip(samples, -1.0, 1.0) * 32767).astype("<i2").tobytes())
    sys.stdout.buffer.flush()
""".trimIndent()
}
