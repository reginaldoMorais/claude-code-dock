package dev.reginaldomorais.claudedock.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** T-1.79+: as vozes vêm da fonte, nos dois motores (RF-55, RF-56, D-46). */
class KokoroVoicesTest {

    /** O `voices-*.bin` é um `.npz`, que é um ZIP: cada entrada é uma voz. */
    @Test
    fun `lista as vozes do arquivo, ordenadas por idioma`() {
        val file = voicesArchive("pf_dora.npy", "af_heart.npy", "pm_alex.npy")

        // 'a' é en-us e vem antes de 'p', que é pt-br; dentro do idioma, ordem alfabética.
        assertEquals(listOf("af_heart", "pf_dora", "pm_alex"), KokoroVoices.listVoices(file.path))
    }

    /** Combo vazio é uma tela ruim; stack trace na tela de settings é pior. */
    @Test
    fun `arquivo ausente ou corrompido devolve lista vazia`() {
        assertEquals(emptyList<String>(), KokoroVoices.listVoices("/tmp/nao-existe-xyz.bin"))
        assertEquals(emptyList<String>(), KokoroVoices.listVoices("   "))

        val naoZip = File.createTempFile("voices", ".bin").apply {
            writeText("isto não é um zip")
            deleteOnExit()
        }
        assertEquals(emptyList<String>(), KokoroVoices.listVoices(naoZip.path))
    }

    /** O prefixo do nome é a convenção do próprio Kokoro, e é o que define o idioma. */
    @Test
    fun `idioma vem do prefixo da voz`() {
        assertEquals("pt-br", KokoroVoices.languageOf("pf_dora"))
        assertEquals("pt-br", KokoroVoices.languageOf("pm_santa"))
        assertEquals("en-us", KokoroVoices.languageOf("af_heart"))
        assertEquals("en-gb", KokoroVoices.languageOf("bm_george"))
        assertEquals("es", KokoroVoices.languageOf("ef_dora"))
        assertEquals("ja", KokoroVoices.languageOf("jf_alpha"))
    }

    /** Prefixo desconhecido cai no default do próprio `create()`, e não em exceção. */
    @Test
    fun `prefixo desconhecido cai no idioma padrao`() {
        assertEquals(KokoroVoices.DEFAULT_LANGUAGE, KokoroVoices.languageOf("xx_foo"))
        assertEquals(KokoroVoices.DEFAULT_LANGUAGE, KokoroVoices.languageOf(""))
    }

    @Test
    fun `rotulo mostra a voz e o idioma`() {
        assertEquals("pf_dora — pt-br", KokoroVoices.label("pf_dora"))
    }

    private fun voicesArchive(vararg entries: String): File =
        File.createTempFile("voices", ".bin").apply {
            ZipOutputStream(outputStream()).use { zip ->
                entries.forEach { name ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(ByteArray(4))
                    zip.closeEntry()
                }
            }
            deleteOnExit()
        }
}

/** T-1.82+: o sidecar do Piper descreve a voz — inclusive a taxa, que varia (DEF-10). */
class PiperVoicesTest {

    /**
     * **O teste do bug de produção.** As vozes `low`/`x_low` do Piper são 16000 Hz, e o plugin
     * tinha 22050 fixo no código: 40 das 173 vozes do índice oficial tocavam 38% aceleradas e com
     * o tom alterado. Uma delas é pt-BR (`pt_BR-edresson-low`).
     */
    @Test
    fun `taxa de voz low vem do sidecar, e nao da constante`() {
        val voice = voiceWithSidecar(
            "pt_BR-edresson-low",
            """{"audio": {"sample_rate": 16000, "quality": "low"}}""",
        )

        assertEquals(16000f, PiperVoices.sampleRate(voice.path))
    }

    @Test
    fun `taxa de voz medium continua em 22050`() {
        val voice = voiceWithSidecar(
            "pt_BR-faber-medium",
            """{"audio": {"sample_rate": 22050, "quality": "medium"}}""",
        )

        assertEquals(22050f, PiperVoices.sampleRate(voice.path))
    }

    /** Sem sidecar, ou com sidecar ilegível, a fala precisa sair mesmo assim (D-49). */
    @Test
    fun `sem sidecar cai na taxa padrao`() {
        val semSidecar = File.createTempFile("voz", ".onnx").apply { deleteOnExit() }

        assertEquals(PiperVoices.DEFAULT_SAMPLE_RATE, PiperVoices.sampleRate(semSidecar.path))
        assertEquals(PiperVoices.DEFAULT_SAMPLE_RATE, PiperVoices.sampleRate("/tmp/nao-existe.onnx"))

        val quebrado = voiceWithSidecar("quebrada", "{ isto não é json }")
        assertEquals(PiperVoices.DEFAULT_SAMPLE_RATE, PiperVoices.sampleRate(quebrado.path))
    }

    @Test
    fun `rotulo traz idioma e pais, e cai no nome sem sidecar`() {
        val comIdioma = voiceWithSidecar(
            "pt_BR-faber-medium",
            """{"language": {"name_english": "Portuguese", "country_english": "Brazil"}}""",
        )
        assertEquals("pt_BR-faber-medium — Portuguese (Brazil)", PiperVoices.label(comIdioma))

        val semSidecar = File(comIdioma.parentFile, "sem_sidecar.onnx").apply {
            createNewFile()
            deleteOnExit()
        }
        assertEquals("sem_sidecar", PiperVoices.label(semSidecar))
    }

    /** As vozes instaladas são os `.onnx` irmãos do modelo configurado. */
    @Test
    fun `lista os onnx irmaos do modelo configurado`() {
        val dir = createTempDir()
        listOf("a-medium.onnx", "b-low.onnx", "leia-me.txt").forEach {
            File(dir, it).createNewFile()
        }

        val encontrados = PiperVoices.listModels(File(dir, "a-medium.onnx").path).map { it.name }

        assertEquals(listOf("a-medium.onnx", "b-low.onnx"), encontrados)
    }

    @Test
    fun `caminho vazio ou inexistente devolve lista vazia`() {
        assertTrue(PiperVoices.listModels("   ").isEmpty())
        assertTrue(PiperVoices.listModels("/tmp/pasta-que-nao-existe-xyz/voz.onnx").isEmpty())
    }

    private fun voiceWithSidecar(name: String, sidecar: String): File {
        val dir = createTempDir()
        val voice = File(dir, "$name.onnx").apply { createNewFile() }
        File(dir, "$name.onnx.json").writeText(sidecar)
        return voice
    }

    private fun createTempDir(): File =
        java.nio.file.Files.createTempDirectory("piper-voices").toFile().apply { deleteOnExit() }
}
