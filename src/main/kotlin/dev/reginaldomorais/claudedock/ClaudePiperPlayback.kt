package dev.reginaldomorais.claudedock

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import kotlin.math.min

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
     * Sintetiza texto e retorna PCM cru (22050 Hz, 16-bit, mono).
     * Fora da EDT. Lança ProcessOutput, capturando stderr. Timeout 20s.
     * Nenhum log do texto (RNF-21).
     */
    fun synthesize(text: String, executable: String, modelPath: String): ByteArray? {
        if (text.isBlank()) return null

        return try {
            val cmd = GeneralCommandLine(executable)
                .withParameters("-m", modelPath, "--output-raw")
                .withCharset(Charsets.UTF_8)

            val process = cmd.createProcess()
            val stdin = process.outputStream
            stdin.write(text.toByteArray(Charsets.UTF_8))
            stdin.close()

            val stdoutBytes = process.inputStream.readBytes()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                LOG.warn("Piper synthesis failed with exit code $exitCode")
                return null
            }

            stdoutBytes.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            LOG.warn("Failed to synthesize with Piper", e)
            null
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
