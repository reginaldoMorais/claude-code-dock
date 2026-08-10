package dev.reginaldomorais.claudedock.settings

import java.io.File
import java.util.zip.ZipFile

/**
 * Vozes do Kokoro, lidas do próprio arquivo de vozes (RF-55, D-46).
 *
 * O `voices-v1.0.bin` é um `.npz`, que é um ZIP: cada entrada é `<voz>.npy`. Ler o arquivo custou
 * 0,40–2,20 ms medidos, e acontece uma vez por abertura da tela — barato o bastante para dispensar
 * uma tabela fixa que envelheceria a cada release de vozes.
 */
object KokoroVoices {

    /**
     * Prefixo da voz → código de idioma do espeak-ng.
     *
     * É a convenção de nomes do próprio Kokoro (`pf_dora` é português, `af_heart` é inglês
     * americano). Só isto é fixo: os nomes das vozes vêm do arquivo.
     */
    private val LANGUAGES = mapOf(
        'a' to "en-us",
        'b' to "en-gb",
        'e' to "es",
        'f' to "fr-fr",
        'h' to "hi",
        'i' to "it",
        'j' to "ja",
        'p' to "pt-br",
        'z' to "cmn",
    )

    /** Idioma default do `create()` do Kokoro, usado quando o prefixo é desconhecido. */
    const val DEFAULT_LANGUAGE = "en-us"

    /**
     * Vozes disponíveis, ordenadas por idioma e depois por nome — é o que agrupa os idiomas no
     * combo sem precisar de uma árvore.
     *
     * Devolve lista vazia em qualquer falha: a tela mostra um combo vazio, e não um stack trace.
     */
    fun listVoices(voicesPath: String): List<String> {
        val file = File(voicesPath.trim())
        if (!file.isFile || !file.canRead()) return emptyList()

        return try {
            ZipFile(file).use { zip ->
                zip.entries().asSequence()
                    .map { it.name.removeSuffix(".npy") }
                    .filter { it.isNotBlank() }
                    .sortedWith(compareBy({ languageOf(it) }, { it }))
                    .toList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Idioma da voz pelo prefixo. Prefixo desconhecido cai no default do próprio `create()`. */
    fun languageOf(voice: String): String =
        LANGUAGES[voice.trim().firstOrNull()?.lowercaseChar()] ?: DEFAULT_LANGUAGE

    /** Rótulo do combo: `pf_dora — pt-br`. */
    fun label(voice: String): String = "$voice — ${languageOf(voice)}"
}

/**
 * Vozes do Piper, lidas dos arquivos instalados ao lado do modelo configurado (RF-56, DEF-10).
 *
 * Cada voz do Piper é um par `<voz>.onnx` + `<voz>.onnx.json`, e o sidecar traz idioma, qualidade
 * e **sample rate**. Esse último é o motivo de este objeto existir: a taxa varia (16000 Hz em
 * `low`/`x_low`, 22050 Hz em `medium`/`high`) e o plugin a tinha fixa em 22050, o que tocava 40
 * das 173 vozes do índice oficial 38% aceleradas e com o tom alterado.
 */
object PiperVoices {

    /** Taxa das vozes `medium`/`high`, usada quando o sidecar falta ou não casa. */
    const val DEFAULT_SAMPLE_RATE = 22050f

    /**
     * Os dois campos são lidos por regex de propósito (D-49): não há parser JSON na plataforma —
     * nem Gson nem Jackson nos jars de `lib/` — e a alternativa seria uma dependência nova no
     * plugin para extrair um inteiro e dois rótulos de um arquivo gerado por pipeline.
     */
    private val SAMPLE_RATE = Regex(""""sample_rate"\s*:\s*(\d+)""")
    private val NAME_ENGLISH = Regex(""""name_english"\s*:\s*"([^"]*)"""")
    private val COUNTRY_ENGLISH = Regex(""""country_english"\s*:\s*"([^"]*)"""")

    /**
     * Vozes instaladas: os `.onnx` irmãos do modelo configurado, ordenados por nome.
     *
     * O diretório vem do próprio `piperModel` em vez de um campo novo — assim a configuração
     * existente continua valendo sem migração.
     */
    fun listModels(modelPath: String): List<File> {
        val parent = File(modelPath.trim()).parentFile ?: return emptyList()
        if (!parent.isDirectory) return emptyList()

        return parent.listFiles { f -> f.isFile && f.name.endsWith(".onnx") }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    /** Rótulo do combo: `pt_BR-faber-medium — Portuguese (Brazil)`. Sem sidecar, só o nome. */
    fun label(model: File): String {
        val name = model.name.removeSuffix(".onnx")
        val json = sidecarText(model.path) ?: return name

        val language = NAME_ENGLISH.find(json)?.groupValues?.get(1) ?: return name
        val country = COUNTRY_ENGLISH.find(json)?.groupValues?.get(1)

        return if (country.isNullOrBlank()) "$name — $language" else "$name — $language ($country)"
    }

    /**
     * Sample rate declarado pela voz. Cai em [DEFAULT_SAMPLE_RATE] quando o sidecar falta, não é
     * legível, ou não traz o campo.
     */
    fun sampleRate(modelPath: String): Float {
        val json = sidecarText(modelPath) ?: return DEFAULT_SAMPLE_RATE
        val declared = SAMPLE_RATE.find(json)?.groupValues?.get(1)?.toFloatOrNull()

        return declared?.takeIf { it > 0f } ?: DEFAULT_SAMPLE_RATE
    }

    private fun sidecarText(modelPath: String): String? {
        val sidecar = File("${modelPath.trim()}.json")
        if (!sidecar.isFile || !sidecar.canRead()) return null

        return try {
            sidecar.readText()
        } catch (e: Exception) {
            null
        }
    }
}
