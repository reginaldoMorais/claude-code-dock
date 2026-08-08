package dev.reginaldomorais.claudedock

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings
import java.io.File
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/**
 * ÚNICO ponto de acoplamento com Piper TTS e javax.sound.sampled (RNF-19).
 *
 * Responsabilidades:
 * - Validar que executável e modelo existem
 * - Lançar `piper` com texto, ler PCM do stdout
 * - Reproduzir PCM via Clip
 * - Pausar, retomar, parar
 * - Limpar recursos de áudio (RNF-22)
 *
 * Nenhum acoplamento a Project, serviços, actions, ou UI. Lógica pura.
 */
object ClaudePiperPlayback {

    private val LOG = Logger.getInstance(ClaudePiperPlayback::class.java)

    private var currentClip: Clip? = null
    private var currentProcess: Process? = null

    /** Prazo da síntese (RNF-20). Constante em código: é medida de implementação, não preferência. */
    private const val TIMEOUT_SECONDS = 20L

    /**
     * Verifica se síntese é possível: executável existe E modelo (.onnx) existe.
     * Fora da EDT. Sem exceções.
     */
    fun canSynthesize(executable: String, modelPath: String): Boolean {
        if (executable.isBlank() || modelPath.isBlank()) return false

        val trimmedExe = executable.trim()
        val trimmedModel = modelPath.trim()

        // Modelo deve existir e ser arquivo legível
        val modelFile = File(trimmedModel)
        if (!modelFile.isFile || !modelFile.canRead()) return false

        // Executável: se for caminho explícito (contém /), validar diretamente
        if (trimmedExe.contains("/")) {
            val exeFile = File(trimmedExe)
            if (exeFile.isFile && exeFile.canExecute()) return true
        }

        // Executável: consultar PATH via IntelliJ (padronizado com claude)
        if (com.intellij.execution.configurations.PathEnvironmentVariableUtil.findInPath(trimmedExe) != null) return true

        // Fallback: ~/. local/bin, /usr/local/bin
        val fallbacks = listOf(
            File(System.getProperty("user.home"), ".local/bin").path,
            "/usr/local/bin",
        )
        return fallbacks.any { dir -> File(dir, trimmedExe).let { it.isFile && it.canExecute() } }
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
     * Sintetiza texto e retorna PCM cru (22050 Hz, 16-bit, mono).
     * Fora da EDT. Nenhum log do texto (RNF-21).
     *
     * O processo fica em `currentProcess` enquanto roda: é o que permite a `stop()` abortar uma
     * síntese em curso (RNF-23). Antes da correção do Achado 31 o campo nunca era atribuído, e o
     * `destroy()` de `stopCurrent` operava sempre sobre `null` — o piper seguia até o fim.
     *
     * A leitura do stdout roda em outra thread **de propósito**: `readBytes()` só volta no EOF, e
     * o piper trava se o buffer do pipe encher sem ninguém lendo. Um `waitFor` com prazo depois de
     * uma leitura bloqueante não limitaria coisa nenhuma (RNF-20).
     */
    fun synthesize(
        text: String,
        executable: String,
        modelPath: String,
        speedPercent: Int = ClaudeDockSettings.DEFAULT_SPEECH_SPEED,
        timeoutSeconds: Long = TIMEOUT_SECONDS,
    ): ByteArray? {
        if (text.isBlank()) return null

        var launched: Process? = null
        return try {
            val cmd = GeneralCommandLine(executable)
                .withParameters(piperParameters(modelPath, speedPercent))
                .withCharset(Charsets.UTF_8)

            val process = cmd.createProcess()
            launched = process
            currentProcess = process

            val stdout = CompletableFuture.supplyAsync { process.inputStream.readBytes() }
            process.outputStream.use { it.write(text.toByteArray(Charsets.UTF_8)) }

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                LOG.warn("Piper synthesis timed out after ${timeoutSeconds}s")
                process.destroyForcibly()
                return null
            }

            val exitCode = process.exitValue()
            if (exitCode != 0) {
                // Também é o caminho de uma síntese cancelada por stop(): o destroy mata o piper
                // e ele sai com código diferente de zero.
                LOG.warn("Piper synthesis failed with exit code $exitCode")
                return null
            }

            stdout.get().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            LOG.warn("Failed to synthesize with Piper", e)
            null
        } finally {
            // Só limpa se ninguém tiver começado outra síntese no meio-tempo.
            if (currentProcess === launched) currentProcess = null
        }
    }

    /**
     * Reproduz PCM cru em 22050 Hz, 16-bit, mono.
     * Abre Clip, inicia, libera recurso em finally (RNF-22).
     * Fora da EDT (chamador é responsável).
     */
    fun playBytes(pcmBytes: ByteArray): Boolean {
        if (pcmBytes.isEmpty()) return false

        stopCurrent()  // Parar reprodução anterior (RNF-23)

        return try {
            val format = AudioFormat(22050f, 16, 1, true, false)  // 22050 Hz, 16-bit, mono, signed, little-endian
            val audioStream = AudioInputStream(
                java.io.ByteArrayInputStream(pcmBytes),
                format,
                (pcmBytes.size / format.frameSize).toLong()
            )

            val clip = AudioSystem.getClip()
            clip.open(audioStream)

            currentClip = clip
            clip.start()
            true
        } catch (e: Exception) {
            LOG.warn("Failed to play audio", e)
            stopCurrent()  // Limpar em caso de erro
            false
        }
    }

    fun pause() {
        currentClip?.let { if (it.isRunning) it.stop() }
    }

    fun resume() {
        currentClip?.let { if (!it.isRunning && it.frameLength > 0) it.start() }
    }

    fun stop() {
        stopCurrent()
    }

    private fun stopCurrent() {
        try {
            currentClip?.let {
                if (it.isRunning) it.stop()
                it.close()
            }
        } catch (e: Exception) {
            LOG.warn("Error closing audio clip", e)
        } finally {
            currentClip = null
        }

        try {
            currentProcess?.destroy()
        } catch (e: Exception) {
            LOG.warn("Error destroying piper process", e)
        } finally {
            currentProcess = null
        }
    }

    fun isPlaying(): Boolean = currentClip?.isRunning == true

    fun isPaused(): Boolean = currentClip != null && !currentClip!!.isRunning && currentClip!!.frameLength > 0
}
