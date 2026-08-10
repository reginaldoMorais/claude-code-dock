# Plano — Kokoro-ONNX como motor de TTS, com reprodução em streaming

> Rascunho de planejamento. **Não é o SPEC.** O `plans/sdd/SPEC.md` e o
> `plans/sdd/HANDOFF.md` só são atualizados depois da aprovação deste documento.

## Contexto

O TTS do plugin hoje é o Piper: `ClaudePiperPlayback` lança o binário `piper`, escreve o texto no
stdin, lê o PCM do stdout **inteiro**, e só então abre um `Clip` e toca. O Kokoro-ONNX entrega
vozes bem melhores, já está instalado e testado na máquina do autor, e traz 54 vozes em 9 idiomas.

O pedido: trocar o motor, configurável pela tela de settings, com as três vozes pt-BR, mantendo a
velocidade de fala. Bônus pedidos e **ambos aceitos**: outros idiomas e coexistência com o Piper.

As rodadas de perguntas mudaram o eixo do plano duas vezes, e as duas vezes por medição:

1. **O gargalo nunca foi o motor — é o `Clip` esperar a síntese inteira antes de emitir som.** Os
   dois motores já produzem PCM incrementalmente, e o desenho de v1.5 joga isso fora.
2. **O Piper também merece seletor de voz** — e ao ler o arquivo que descreve a voz aparece um bug
   que existe hoje em produção: 40 das 173 vozes do Piper tocariam no tom errado.

---

## Perguntas respondidas com medição

Tudo medido nesta máquina (12 núcleos, CPU, sem GPU).

### 1. "Esperar quase 10 s pelo primeiro áudio é muito" — de onde veio esse número?

**De lugar nenhum: não existe nas medições.** Os 17,71 s da tabela anterior eram o desenho
*atual*, de bloco único — o problema, não a proposta. Com o script em pedaços, medido nos dois
motores e em três tamanhos de texto:

| Texto | chars | **Piper** | Kokoro, pedaços iguais | **Kokoro, com corte** |
| ----- | ----- | --------- | ---------------------- | --------------------- |
| curto | 56    | 2,02 s    | 2,58 s                 | **2,56 s**            |
| médio | 202   | 2,09 s    | 3,74 s                 | **2,48 s**            |
| longo | 808   | 3,13 s    | 3,92 s                 | **2,41 s**            |

Duas leituras importam mais que os números soltos:

- **O tempo até falar é praticamente constante nos dois motores**, e não cresce com o tamanho do
  trecho. Selecionar um parágrafo ou a tela inteira dá quase o mesmo tempo de espera.
- Com o corte da pergunta 8, **a diferença entre os motores cai para ~0,4 s** — e no texto longo o
  Kokoro passa na frente.

O texto curto quase não muda, e isso está certo: 56 chars já cabem no primeiro pedaço, não há o que
dividir.

> A coluna da direita foi medida ponta a ponta com a variante **B** (rampa). A estratégia escolhida
> depois, **C**, mede 0,72 s contra 0,73 s no primeiro pedaço — a diferença fica dentro do ruído,
> então esses números valem para as duas. A medição direta de C está na pergunta 8.

### 2. Então é melhor usar o Piper?

**Com a rampa, deixou de ser uma escolha de desempenho.** A diferença cai para ~0,4 s no texto
curto e médio, e no texto longo o Kokoro fica **na frente**. O plano mantém os dois de qualquer
forma (decisão A), então a escolha volta a ser o que motivou o pedido: qualidade de voz.

O Piper ainda usa ~3× menos CPU (RTF 0,11 contra 0,35) — o que importa se a máquina estiver sob
carga, não para o tempo de resposta.

Vale saber de onde vinha a diferença: **quase toda era custo fixo de partida**, não síntese — o
`import` do Python (0,44 s) mais a carga do modelo de 325 MB (1,05 s), medidos com variação de
0,92 s a 1,50 s conforme o cache de página do SO. Esse custo **não encolhe** dentro deste plano: o
daemon foi recusado (pergunta 5), o modelo menor saiu mais lento (pergunta 7) e o cache de grafo
otimizado não paga (pergunta 8). O que a rampa faz é tirar do caminho crítico o **outro** termo, o
da síntese do primeiro pedaço.

Minha recomendação: **Kokoro como padrão**. Trocar para o Piper é um combo na tela.

### 3. Dá para listar as vozes do Piper também?

**Dá, e a resposta rendeu mais do que a pergunta.** Cada voz do Piper é um par
`<voz>.onnx` + `<voz>.onnx.json`, e o sidecar descreve a voz — verificado no seu
`pt_BR-faber-medium.onnx.json`:

```json
{ "language": { "code": "pt_BR", "name_english": "Portuguese", "country_english": "Brazil" },
  "audio": { "sample_rate": 22050, "quality": "medium" }, "espeak": { "voice": "pt-br" } }
```

Então basta varrer o diretório do modelo configurado por `*.onnx` e ler o sidecar de cada um: o
Piper ganha o mesmo seletor de voz do Kokoro, rotulado por idioma. O índice oficial tem **173
vozes em 54 idiomas** (5 em português: `pt_BR-cadu-medium`, `pt_BR-edresson-low`,
`pt_BR-faber-medium`, `pt_BR-jeff-medium`, `pt_PT-tugão-medium`).

**E aí aparece o bug.** O `sample_rate` está no sidecar porque ele *varia*, e o código do plugin
tem 22050 fixo em `ClaudePiperPlayback.kt:156`. Verificado baixando um sidecar de cada qualidade
do índice oficial:

| Qualidade | Sample rate | Vozes no índice |
| --------- | ----------- | --------------- |
| `x_low`   | **16000 Hz** | 14             |
| `low`     | **16000 Hz** | 26             |
| `medium`  | 22050 Hz     | 119            |
| `high`    | 22050 Hz     | 14             |

**40 das 173 vozes tocariam 38% mais rápido e com o tom alterado**, e uma delas é pt-BR
(`pt_BR-edresson-low`). Você não esbarrou nisso porque a sua voz é `medium`. Ler o sidecar conserta
de raiz, e é o mesmo trabalho de listar as vozes.

### 4. Vale trocar a leitura do ZIP por um enum com as 3 vozes pt-BR?

**Não — o custo é irrisório.** `java.util.zip.ZipFile` sobre o `voices-v1.0.bin` de 28 MB: **2,20
ms** na primeira leitura, **0,40 ms** com o cache do SO quente, uma vez ao abrir a tela de
configuração. Por 2 ms ficam as 54 vozes em vez de 3, sem tabela para envelhecer. Um enum de 3
entradas custaria mais linhas do que a leitura do ZIP.

### 5. Carregar o modelo ao abrir o plugin ajuda? Quanto custa de memória? Vale o daemon?

O modelo vive num processo **Python**, então as duas perguntas são a mesma: é o daemon.

| Etapa                    | Tempo  | RSS         |
| ------------------------ | ------ | ----------- |
| Baseline do Python       | —      | 11 MB       |
| `import` das bibliotecas | 0,44 s | 77 MB       |
| Carga do modelo f32      | 1,16 s | **496 MB**  |
| Depois de síntese longa  | —      | **961 MB**  |

**Recusado.** Ele compra os 1,60 s de custo fixo e cobra ~500 MB residentes parados (pico de 961
MB), protocolo de IPC, ciclo de vida e risco de zumbi. Ganho medido: 2,5× em texto curto, 1,4× em
médio, **1,1× em longo** — some justamente onde a espera incomoda. Com streaming, esses 1,60 s são
a diferença entre falar em 3,8 s e falar em 2,2 s. Se um dia o uso for quase só de frases curtas,
o daemon volta como mudança aditiva.

### 6. Vale um CLI próprio para o Kokoro?

**Não.** Seria o mesmo script de 18 linhas morando em outro lugar: não acelera nada e cria duas
coisas que hoje não existem — um passo de instalação para o usuário e defasagem de versão entre
plugin e CLI ("o plugin 0.10 exige o kokoro-cli 0.2"). O `python3 -c` não tem instalação nem
defasagem: o script sai sempre do JAR que está rodando. Isso mudaria se houvesse daemon.

### 7. Vale um modelo menor? **Medido: não.**

Com o int8 instalado por você, medido nas três configurações de thread, mesmo texto e mesma voz:

| Modelo          | Carga  | RSS na carga | threads default | **4 threads** | 12 threads |
| --------------- | ------ | ------------ | --------------- | ------------- | ---------- |
| f32 (325,5 MB)  | 1,20 s | 483 MB       | RTF 0,40        | **RTF 0,34**  | RTF 0,40   |
| int8 (92,4 MB)  | 0,80 s | 216 MB       | RTF 2,30        | **RTF 1,55**  | RTF 1,74   |

**O int8 é ~4,5× mais lento, não mais rápido.** E o número que encerra a discussão: o melhor RTF
dele é **1,55 — mais lento que tempo real**. Com streaming, a reprodução alcançaria a síntese e o
áudio engasgaria. Ele se desqualifica pela velocidade, antes mesmo de discutir qualidade.

Isso não é anomalia de configuração: reproduz nos três ajustes de thread. A explicação provável é
a de sempre em quantização dinâmica — as camadas do StyleTTS2 caem em kernels sem caminho int8
otimizado no onnxruntime de CPU, e cada operação paga dequantização/requantização. **Verifiquei o
efeito, não a causa** — e a causa não muda a decisão.

O que o int8 entrega de fato: **2,2× menos memória** (216 MB contra 483 MB na carga) e 0,4 s a
menos de partida. Não paga 4,5× de velocidade.

**Fica o f32.** Amostras de áudio das duas versões estão em `amostra_f32.wav` e `amostra_int8.wav`
no scratchpad, se você quiser ouvir a diferença de qualidade — mas ela não muda a decisão.

O fp16 não foi medido e não vale medir pelo mesmo motivo estrutural: o release marca o asset fp16
como `-gpu`, e em CPU o onnxruntime converte fp16 para fp32 em tempo de execução.

### 8. "Quero reduzir esses 4 s, nem que seja para 3" — **entregue: 2,4 s**

Primeiro medi **onde** o tempo é gasto, em vez de chutar otimização. Custo fixo, etapa por etapa:

| Etapa                             | Tempo      |
| --------------------------------- | ---------- |
| `import numpy`                    | 0,102 s    |
| `import onnxruntime`              | 0,025 s    |
| `import kokoro_onnx`              | 0,327 s    |
| **`InferenceSession`** (o modelo) | **1,046 s** |
| vozes, vocabulário, tokenizer     | 0,003 s    |
| **fixo total**                    | **1,503 s** |

E o primeiro pedaço, que é o outro termo, cresce **linearmente** com o tamanho:

| 1º pedaço | 30 chars | 60 chars | 100 chars | 200 chars |
| --------- | -------- | -------- | --------- | --------- |
| síntese   | 0,65 s   | 1,28 s   | 2,07 s    | 3,32 s    |

Aí está a resposta: `1,50 + 2,07 = 3,57 s` era exatamente o que se media, porque a primeira frase
do texto médio tem ~108 chars. **O tempo até falar nunca dependeu do trecho inteiro — só da
primeira frase.**

**A correção é cortar o primeiro pedaço por palavra.** Comparei três estratégias no mesmo texto —
a terceira é a sua, do teste em outra janela, mais essa regra:

| Estratégia                                  | pedaços | 1º pedaço  | síntese total | folga mín. |
| ------------------------------------------- | ------- | ---------- | ------------- | ---------- |
| **A** — corta em toda pontuação (a sua)     | 16      | 1,81 s     | 14,79 s       | +5,63 s    |
| **B** — rampa dobrando `(40, 80, 160)`      | 5       | **0,73 s** | 14,56 s       | +2,45 s    |
| **C** — a sua **+ teto de palavras no 1º**  | 17      | **0,72 s** | 14,79 s       | +2,45 s    |

**Fica a C.** Três conclusões que a tabela força:

- **O ganho inteiro vem de uma regra só**: cortar o primeiro pedaço por palavra (1,81 → 0,72 s).
- **B e C empatam**, e C é bem mais simples — sem rampa, sem tupla de tetos, sem índice.
- **O número de pedaços não afeta o tempo total** (16 contra 5 dá o mesmo). Isso derruba a
  justificativa que eu tinha escrito para a rampa, de que blocos maiores seriam mais eficientes.
  Não são. **A rampa não pagava o próprio custo, e saiu do plano.**

A folga de produção nunca fica negativa em nenhuma das três, então nada engasga. Vale notar que é
a mesma medição que desqualifica o int8: com RTF 1,55 nenhuma estratégia de corte funcionaria,
porque a reprodução sempre alcançaria a síntese.

**Amostras para julgar de ouvido** (o cronômetro empatou; o critério que sobra é o seu ouvido),
no scratchpad: `corte_A_toda_pontuacao.wav`, `corte_B_rampa.wav`, `corte_C_simples.wav` e
`corte_D_sem_corte.wav` — a última é a referência sem corte nenhum.

**Duas otimizações medidas e recusadas**, para não voltarem depois:

- **Cache de grafo otimizado do ONNX** (`optimized_model_filepath`): carga 1,157 s → 0,946 s.
  **0,2 s** ao preço de mais 325 MB em disco e de um artefato que o próprio onnxruntime avisa ser
  específico do hardware em que foi gerado. A carga é I/O de 325 MB, não otimização de grafo.
- **Sobrepor a fonemização com a carga do modelo:** as duas são independentes, mas fonemizar 40
  chars custa dezenas de milissegundos contra 1,05 s de carga. Não há o que sobrepor.

**Se um dia 2,4 s ainda incomodar,** a saída que resta é manter o processo vivo por alguns segundos
depois de uma fala, para que a *próxima* pague só a síntese (~0,65 s). É o daemon com prazo de
validade — fora do escopo desta rodada, e listado aqui para não se perder.

---

## Fatos medidos que decidem o desenho

| Fato                                        | Medição                                                                                                    |
| ------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| **`kokoro-onnx` não tem CLI**               | `kokoro-onnx 0.5.0`, `entry_points: []`. A integração passa por um interpretador Python                       |
| **Threads do ONNX decidem a viabilidade**   | 724 chars: **65,35 s** no default → **15,45 s** com `intra_op_num_threads=4`. RTF 1,56 → 0,37. 12 threads é pior |
| **O modelo int8 é 4,5× mais lento**         | RTF 1,55 contra 0,34 do f32, nas três configurações de thread. Acima de 1,0 = mais lento que tempo real         |
| **Sample rate do Kokoro é 24000 Hz**        | `kokoro_onnx/config.py: SAMPLE_RATE = 24000`                                                                 |
| **Sample rate do Piper varia**              | `low`/`x_low` = 16000 Hz, `medium`/`high` = 22050 Hz. O código tem 22050 fixo                                 |
| **Saída do Kokoro é `float32`**             | `create()` devolve `NDArray[np.float32]` normalizado — vira `int16` little-endian antes do áudio              |
| **`create()` recusa velocidade < 0,5**      | `assert speed >= 0.5 and speed <= 2.0` em `kokoro_onnx/__init__.py:183`                                       |
| **`voices-v1.0.bin` é um ZIP**              | Magic `PK\x03\x04`, 54 entradas `.npy`. Listagem na JVM em 0,40–2,20 ms                                       |
| **Os dois motores emitem incrementalmente** | 1º áudio praticamente constante: Piper ~2,1 s, Kokoro **2,41–2,56 s** com a rampa                             |
| **O 1º áudio depende só da 1ª frase**       | Custo fixo 1,50 s (1,05 s é a carga do modelo) + síntese linear: 0,65 s a cada 30 chars do 1º pedaço          |
| **Nenhum dos dois usa stderr**              | 0 bytes no caminho feliz. Drenar stderr é diagnóstico, **não** conserto de deadlock                           |
| **Sem parser JSON na plataforma**           | Nem Gson nem Jackson nos jars de `lib/` do IDE instalado — ler o sidecar não pode assumir um parser           |
| **`espeak-ng` presente**                    | `/usr/bin/espeak-ng` 1.51; `pt-br`, `en-us`, `en-gb`, `es`, `fr-fr`, `it`, `hi`, `ja`, `cmn` disponíveis      |

---

## Decisões

| #   | Decisão                                                    | Por quê                                                                                                                                          |
| --- | ---------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| A   | **Dois motores**, `TtsEngine.KOKORO` como padrão            | Custa um enum e um `when` em três pontos — não uma interface com fábrica para duas implementações                                                 |
| B   | **Streaming com `SourceDataLine`** nos dois motores         | É a otimização que o daemon tentava comprar, e ~3× maior. Beneficia o Piper de graça                                                              |
| B2  | **Corte em pontuação + teto de palavras no 1º pedaço**      | O tempo até falar depende só da primeira frase, não do trecho. O teto leva o 1º pedaço de 1,81 s para **0,72 s**; rampa dobrando foi medida e não pagou |
| C   | **Sem daemon**                                              | 1,60 s comprados por ~500 MB residentes                                                                                                          |
| D   | **Script Python embutido** via `python3 -c "<script>"`      | Sem arquivo em `resources`, sem extração, sem cache defasado. ~18 linhas, testável como valor puro                                                |
| E   | **Vozes lidas da fonte, nos dois motores**                  | Kokoro: entradas do ZIP. Piper: `*.onnx` irmãos + sidecar. Nenhuma tabela fixa envelhece                                                          |
| F   | **Sample rate vem da voz**, nunca de constante              | Kokoro 24000; Piper do sidecar com queda para 22050. Conserta as 40 vozes `low`/`x_low` que hoje tocam errado                                     |
| G   | **0,25x sai da tabela** (dos dois motores)                  | O Kokoro asserta 0,5–2,0. `effectiveSpeechSpeed()` já aproxima: quem tem `25` gravado migra sozinho para `50`, sem código de migração             |
| H   | **Renomear `ClaudePiperPlayback` → `ClaudeTtsPlayback`**    | Um arquivo chamado "Piper" que roda Kokoro é a confusão das 3h da manhã. Rename mecânico: 5 chamadas no serviço, 2 em actions, 1 arquivo de teste |
| I   | **Prazo passa a valer até o primeiro byte**, não total      | Com streaming o processo vive legitimamente enquanto o áudio toca (47 s, mais o tempo pausado). Um teto total mataria a fala no meio              |
| J   | **Sidecar lido por regex de dois campos**, sem dependência  | Não há parser JSON na plataforma, e o que se precisa é um inteiro e dois rótulos, de arquivo gerado por pipeline. Queda segura se não casar       |

**Trade-off aceito (B):** trocar `Clip` por `SourceDataLine` mexe no que hoje está estável e
reescreve pause/resume/stop, alterando RNF-22. Em troca some a espera em silêncio nos dois motores
e some o buffer de áudio inteiro na memória da JVM. É a mudança de maior risco da rodada — daí ela
vir com teste próprio de "chegou em pedaços".

**Trade-off aceito (J):** regex sobre JSON é frágil por natureza. Aqui é sobre dois campos de
arquivo gerado por pipeline, com queda para 22050 Hz e para o nome do arquivo; a alternativa era
uma dependência nova no plugin para ler um inteiro.

**Trade-off aceito (D):** o script embutido é pior de debugar que um `.py` no repositório. Em troca
some a classe inteira de bugs de extração e cache. Se passar de ~30 linhas, vira arquivo.

**Fora do escopo:** daemon, download automatizado de modelos/vozes, seleção de voz por idioma
detectado do texto, e fila de falas.

---

## Mudanças

### 1. `settings/ClaudeDockSettings.kt`

```kotlin
enum class TtsEngine { PIPER, KOKORO }

var ttsEngine: TtsEngine = TtsEngine.KOKORO
var kokoroPython: String = "python3"   // interpretador com kokoro-onnx instalado
var kokoroModel: String = ""           // kokoro-v1.0.onnx
var kokoroVoices: String = ""          // voices-v1.0.bin
var kokoroVoice: String = "pf_dora"
```

`piperExecutable` e `piperModel` **não mudam** — o combo de voz do Piper escreve o caminho completo
de volta em `piperModel`, então o seu XML atual do WebStorm (`pt_BR-faber-medium.onnx`,
`speechSpeed=125`) continua valendo sem migração.

`SPEECH_SPEEDS` perde a entrada `25 to "0,25x"` (decisão G).

> **Atenção ao pyenv.** Seu `python3` e seu `piper` são shims (`~/.pyenv/shims/…`), e o PATH que o
> IDE herda pode não ter esse diretório — o `piperExecutable` gravado no seu XML já é o caminho
> absoluto do shim, justamente por isso. Daí o campo do interpretador ter seletor de arquivo.

### 2. `settings/TtsVoices.kt` — **novo**, ~70 linhas, dois objetos

Mora em `settings` pelo mesmo motivo que `SPEECH_SPEEDS`: é o domínio do valor configurado,
consumido pela tela e pela síntese. Dois objetos no mesmo arquivo porque resolvem o mesmo problema
por caminhos diferentes — juntá-los numa abstração só criaria uma interface para dois formatos que
não têm nada em comum além do resultado.

```kotlin
object KokoroVoices {
    /** Prefixo da voz → código de idioma do espeak-ng. */
    private val LANGUAGES = mapOf(
        'a' to "en-us", 'b' to "en-gb", 'e' to "es", 'f' to "fr-fr",
        'h' to "hi", 'i' to "it", 'j' to "ja", 'p' to "pt-br", 'z' to "cmn",
    )
    fun listVoices(voicesPath: String): List<String>  // entradas do ZIP, sem ".npy", ordenadas
    fun languageOf(voice: String): String             // 'p' de "pf_dora" → "pt-br"
    fun label(voice: String): String                  // "pf_dora — pt-br"
}

object PiperVoices {
    fun listModels(modelPath: String): List<File>     // *.onnx irmãos do modelo configurado
    fun label(model: File): String                    // "pt_BR-faber-medium — Portuguese (Brazil)"
    fun sampleRate(modelPath: String): Float          // do sidecar; 22050f se não houver
}
```

`listVoices` e `listModels` devolvem lista vazia em qualquer falha — a tela mostra combo vazio em
vez de estourar. `languageOf` cai em `"en-us"` para prefixo desconhecido, que é o default do
próprio `create()`. `label` e `sampleRate` caem no nome do arquivo e em 22050f quando o sidecar
falta ou não casa com a regex.

### 3. `ClaudePiperPlayback.kt` → `ClaudeTtsPlayback.kt` — o coração da rodada

Continua sendo o único ponto de acoplamento com processo de TTS e `javax.sound.sampled` (RNF-19).

```kotlin
/** Tudo que varia entre motores, e nada além disso. */
internal data class TtsCommand(
    val executable: String,
    val args: List<String>,
    val sampleRate: Float,
    val startTimeoutSeconds: Long,   // prazo até o PRIMEIRO byte (decisão I)
)

internal fun buildCommand(settings: ClaudeDockSettings, speedPercent: Int): TtsCommand
fun canSynthesize(settings: ClaudeDockSettings): Boolean

/** Processo puro, sem áudio nenhum. É o que permite testar headless. */
internal fun stream(text: String, command: TtsCommand, sink: (ByteArray, Int) -> Unit): Boolean

/** Único ponto que toca javax.sound.sampled. Bloqueia enquanto o áudio toca. */
fun speak(text: String, command: TtsCommand): Boolean
```

`synthesize` + `playBytes` somem, substituídos por `stream` + `speak`. A separação entre os dois
mantém a lógica de processo testável sem mixer de áudio — em CI headless o
`AudioSystem.getSourceDataLine` não existe, e sem essa costura os testes T-1.55/T-1.56 morreriam
junto com o `Clip`.

- **`buildCommand`** ramifica no motor. Piper: reaproveita `piperParameters` **sem tocá-la** (o
  `--length-scale` inverso e o `Locale.ROOT` de RNF-31 seguem válidos), sample rate agora vem de
  `PiperVoices.sampleRate(model)`, prazo 20 s. Kokoro:
  `listOf("-c", KOKORO_SCRIPT, model, voices, voice, speed, lang)`, `24000f`, prazo 30 s — folga
  para máquina fria sobre os 1,60 s medidos, e que não precisa mais cobrir o texto inteiro.
- Velocidade do Kokoro é `percent / 100.0` com **`Locale.ROOT`**. RNF-31 vale igual: o
  `float(speed)` do Python recusa vírgula decimal exatamente como o argparse do piper.
- **`canSynthesize`** ramifica: Piper valida executável + `.onnx`; Kokoro valida interpretador +
  `.onnx` + vozes + voz não vazia. A busca de executável (caminho explícito → `findInPath` →
  fallbacks `~/.local/bin` e `/usr/local/bin`, linhas 51–65) vira `private fun hasExecutable()`
  **reaproveitado pelos dois ramos** — mesma regra, não merece cópia.
- **`stream`** mantém o desenho de hoje e conserta o eixo: escreve o texto no stdin numa thread (o
  motivo continua valendo — o processo trava se o pipe encher) e lê o stdout **em laço**,
  entregando cada pedaço ao `sink` em vez de acumular tudo. O `currentProcess` que permite ao
  `stop()` abortar a síntese (Achado 31) fica intacto. O prazo vale até o primeiro byte; depois
  dele o processo pode viver o tempo que a fala durar.
- **`speak`** abre o `SourceDataLine` no sample rate do comando e passa `line::write` como sink. O
  `write` bloqueia quando o buffer enche — contrapressão de graça: o pipe do processo enche e o
  motor para de sintetizar sozinho. `drain()` no fim, `close()` no `finally` (RNF-22).
- **pause/resume/stop**: `line.stop()` / `line.start()` / `line.flush()` + `close()` + `destroy()`
  do processo. Ganho de brinde: pausar a fala agora **pausa também a síntese**, pela mesma
  contrapressão. `isPlaying` vira `line?.isActive == true`.
- **stderr drenado numa thread**, com as últimas linhas no log quando o processo sai diferente de
  zero. Medido: os dois motores escrevem 0 bytes no caminho feliz, então isto **não é conserto de
  deadlock** — é o que hoje faz uma falha do Python virar só "exit code 1" no log (RNF-33).

O script, constante `internal val KOKORO_SCRIPT`:

```python
import sys, re
import numpy as np, onnxruntime as ort
from kokoro_onnx import Kokoro
model, voices, voice, speed, lang = sys.argv[1:6]
o = ort.SessionOptions(); o.intra_op_num_threads = 4; o.inter_op_num_threads = 1
try:
    k = Kokoro.from_session(ort.InferenceSession(model, sess_options=o, providers=["CPUExecutionProvider"]), voices)
except Exception:
    k = Kokoro(model, voices)

def split_chunks(text, first_cap=40):
    """Corta na pontuacao. O 1o pedaco tem teto de palavras: e dele que sai o 1o som."""
    out = [f.strip() for f in re.split(r"(?<=[,.;:!?—\n])\s+", text) if re.search(r"\w", f)]
    if out and len(out[0]) > first_cap * 2:
        w = out[0].split(); head = []
        while w and len(" ".join(head)) < first_cap: head.append(w.pop(0))
        out = [" ".join(head), " ".join(w)] + out[1:]
    return out

for i, chunk in enumerate(split_chunks(sys.stdin.buffer.read().decode("utf-8"))):
    s, _ = k.create(" . " + chunk if i == 0 else chunk, voice=voice, speed=float(speed), lang=lang)
    sys.stdout.buffer.write((np.clip(s, -1.0, 1.0) * 32767).astype("<i2").tobytes())
    sys.stdout.buffer.flush()
```

Cinco coisas neste script não são estilo, são medição:

- **`intra_op_num_threads = 4`** — o 4,2× medido. Fixo em 4 e não em `cpu_count()`: 12 threads
  mediu **pior** (RTF 0,56 contra 0,39).
- **o teto de palavras no primeiro pedaço** — é a regra que carrega o ganho inteiro: **1,81 s →
  0,72 s** no primeiro pedaço. Todo o resto do corte é cosmético.
- **o `flush()` por pedaço** — sem ele o buffer do stdout do Python engole o ganho inteiro.
- **prefixo `" . "` só no primeiro pedaço** — ele existe para não cortar a primeira palavra, e
  repeti-lo em todo pedaço estica a fala: 18,4 s contra 17,8 s no mesmo texto.
- **`from_session` com `try/except`** — é o único jeito de passar `SessionOptions`, e usa
  `session._model_path`, atributo privado do onnxruntime. Verificado em 1.27.0; se quebrar num
  upgrade, o fallback sintetiza 4× mais devagar em vez de não sintetizar.

**Custo honesto do corte:** o Kokoro sintetiza cada pedaço isoladamente, então a prosódia reinicia
a cada corte e a fala estica. O mesmo texto dá **16,1 s numa tacada só** contra **17,8 s cortado**
(+10%). É o preço de não esperar a síntese inteira, e T-3.74 confirma de ouvido — se incomodar,
subir `first_cap` reduz cortes ao custo de ~0,3 s a mais de espera.

### 4. `ClaudeTtaSessions.kt`

`playText` monta o `TtsCommand` uma vez e chama `speak` **na própria thread de pool**, sem o
`invokeLater` de hoje (linha 58): com streaming, a reprodução é o laço de leitura, e ela não pode
morar na EDT. Só a publicação de estado volta para a EDT. `notifyPiperMissing` vira
`notifyEngineMissing`, com o nome do motor ativo na mensagem — continua **no serviço e não nos
chamadores**, pelo motivo já documentado nas linhas 112–123.

### 5. `settings/ClaudeDockConfigurable.kt`

O `group("Piper TTS")` (linhas 79–125) vira `group("Voz (TTS)")` com:

- `comboBox` do motor no topo.
- `rowsRange { … }.visibleIf(…)` para os campos de cada motor — mecanismo do Kotlin UI DSL para
  isso, sem listener manual.
- **Piper:** executável e modelo como hoje, mais um `comboBox` de voz alimentado por
  `PiperVoices.listModels(...)`, rotulado por `PiperVoices.label` e gravando o caminho completo de
  volta em `piperModel`.
- **Kokoro:** interpretador, modelo `.onnx`, arquivo de vozes (todos com
  `textFieldWithBrowseButton`) e o `comboBox` de voz por `KokoroVoices.listVoices(...)`.
  Comentário com o link oficial: <https://github.com/thewh1teagle/kokoro-onnx>.
- A linha de velocidade não muda (continua lendo `SPEECH_SPEEDS`).

### 6. `actions/AudioMenuAction.kt` e `actions/AudioPlayAction.kt`

Chamada nova de `canSynthesize(settings)` e rótulos sem "Piper": `"Áudio (voz não configurada)"`.
Uma linha em cada.

**Arquivos que não mudam:** `ClaudeSelectionCopyButton`, `AudioPauseResumeAction`,
`AudioStopAction`, `SpeechSpeedActions`, `ClaudeToolWindowFactory`, `plugin.xml`. A fatia
engine-neutral do desenho original (`TtsState`, o serviço, a tabela de velocidades) atravessa a
troca de motor **e** a troca de reprodução sem edição — que é o que RNF-19 comprou.

---

## Testes

Suíte atual: 142 verdes, JUnit 4 + `BasePlatformTestCase`, **sem biblioteca de mock** — o padrão é
executável de mentira em disco (`fakePiper`, `ClaudePiperPlaybackTest.kt:143`). O plano segue esse
padrão em vez de introduzir MockK.

**Novos** (`T-1.67` em diante):

| Teste                                                       | Afirma                                                                                                                                                                 |
| ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `ClaudeTtsPlaybackTest` — **áudio chega em pedaços**         | Executável falso que emite 3 rajadas com pausa: o `sink` é chamado 3+ vezes, a 1ª bem antes do fim. **É o teste que falha se alguém devolver o "lê tudo, depois toca"** |
| `ClaudeTtsPlaybackTest` — prazo até o primeiro byte          | Falso que dorme 30 s sem emitir nada aborta no prazo (herda T-1.55)                                                                                                    |
| `ClaudeTtsPlaybackTest` — prazo não mata fala em andamento   | Falso que emite um byte e segue por mais tempo que o prazo **não** é abortado (decisão I)                                                                              |
| `ClaudeTtsPlaybackTest` — `stop` aborta o processo em curso  | Herda T-1.56, agora sobre `stream`                                                                                                                                     |
| `ClaudeTtsPlaybackTest` — `buildCommand` do Kokoro           | `-c`, script e os 5 argumentos na ordem; sample rate 24000                                                                                                             |
| `ClaudeTtsPlaybackTest` — velocidade do Kokoro em JVM pt-BR  | `150` vira `"1.5"` e não `"1,5"` (RNF-31 no motor novo — o bug que o Piper já pagou)                                                                                    |
| `ClaudeTtsPlaybackTest` — `buildCommand` do Piper intacto    | Guarda de regressão: motor `PIPER` segue produzindo `-m/--output-raw/--length-scale`                                                                                   |
| `ClaudeTtsPlaybackTest` — `canSynthesize` do Kokoro          | Falta de qualquer um dos três arquivos, ou voz vazia, recusa                                                                                                           |
| `ClaudeTtsPlaybackTest` — stderr no log da falha             | Falso que escreve em stderr e sai com código 1: a mensagem aparece no log (RNF-33)                                                                                     |
| `PiperVoicesTest` — **sample rate de voz `low`**             | Sidecar com `"sample_rate": 16000` devolve 16000f, **não** 22050f. É o teste do bug de produção                                                                        |
| `PiperVoicesTest` — sem sidecar, ou sidecar quebrado         | Cai em 22050f e no nome do arquivo, sem estourar (decisão J)                                                                                                           |
| `PiperVoicesTest` — lista os `.onnx` irmãos                  | Diretório com 3 `.onnx` + sidecars devolve os 3, ordenados, com rótulo de idioma                                                                                       |
| `KokoroVoicesTest` — lista do ZIP                            | ZIP fabricado com 3 entradas `.npy` devolve as 3 vozes ordenadas, sem sufixo                                                                                           |
| `KokoroVoicesTest` — arquivo ausente/corrompido              | Lista vazia, sem estourar                                                                                                                                              |
| `KokoroVoicesTest` — idioma pelo prefixo                     | `pf_dora`→`pt-br`, `af_heart`→`en-us`, `bm_george`→`en-gb`, `xx_foo`→`en-us`                                                                                           |
| `ClaudeDockSettingsTest` — defaults do Kokoro                | Motor padrão `KOKORO`, voz `pf_dora`, persistência do enum via `loadState`                                                                                             |

**Ajustes em testes existentes:**

- `ClaudeDockSettingsTest` — `SPEECH_SPEEDS` sem `25`; **novo caso**: `speechSpeed = 25` gravado à
  mão aproxima para `50`. É a migração da decisão G, e sem teste ela é só uma esperança.
- `SpeechSpeedActionsTest` — filhos do submenu caem de 8 para 7.
- `ClaudePiperPlaybackTest` → `ClaudeTtsPlaybackTest`, nas assinaturas novas.
- `ClaudeTtaSessionsTest` — `withPiperModel` vira `withEngine`; a notificação cobre o motor ativo.

---

## Verificação

1. **Suíte:** `./gradlew test --rerun` — verdes, zero warnings. Critério que o HANDOFF já usa.
2. **IDE real (`./gradlew runIde`)** — roteiros manuais novos, numeração `T-3.70`+:
   - **T-3.70** — trocar o motor esconde/mostra os campos certos; os dois combos de voz listam o
     que está instalado, rotulado por idioma.
   - **T-3.71** — `pf_dora`, `pm_alex` e `pm_santa` no mesmo trecho: vozes distintas, em pt-BR.
   - **T-3.72** — velocidade 0,5x / 1x / 2x no Kokoro: duração muda na proporção, tom não muda.
   - **T-3.73** — uma voz `en-us` (`af_heart`) e uma `es` (`ef_dora`) pronunciam no idioma certo.
   - **T-3.74** — **streaming + corte:** trecho de ~800 chars começa a falar em **~2,2 s**, não em
     18 s. Critério de aceite de B e B2, com medição para comparar. Avaliar de ouvido se os cortes
     soam naturais; se não, subir `first_cap` troca ~0,3 s de espera por menos cortes.
   - **T-3.75** — **pausa durante a síntese:** pausar no meio de um trecho longo e retomar; o áudio
     continua de onde parou, sem estouro e sem repetir trecho.
   - **T-3.76** — motor `PIPER`: continua tocando, e agora começa em ~2 s (não-regressão + ganho).
   - **T-3.77** — **o bug das vozes `low`:** baixar `pt_BR-edresson-low`
     (`python -m piper.download_voices pt_BR-edresson-low`), selecionar no combo e confirmar que a
     fala sai no tom certo. Antes desta rodada sairia 38% acelerada.
   - **T-3.78** — motor Kokoro com modelo vazio: menu "Áudio" desabilitado e o botão do popup avisa
     em vez de ficar mudo (é o DEF-07 no motor novo).
   - **T-3.79** — parar no meio: o processo Python morre (`pgrep -f kokoro` não sobra nada) e o
     áudio não volta depois.
3. **Sanidade:** trecho de ~800 chars deve **começar** a falar em ~2,4 s no Kokoro e ~2,1 s no
   Piper. Muito acima disso significa que o `flush()` por pedaço, a rampa ou a configuração de
   threads não chegou ao processo — os três sintomas que as medições desta rodada preveem.

---

## Registro no SDD (só depois da aprovação)

**`plans/sdd/SPEC.md` → v1.11:**

- **RF-54** — escolher o motor de voz (Piper ou Kokoro) na tela de configuração.
- **RF-55** — escolher a voz do Kokoro entre as do arquivo de vozes, rotuladas por idioma.
- **RF-56** — escolher a voz do Piper entre as instaladas no diretório do modelo.
- **RF-57** — a fala começa assim que o primeiro pedaço de áudio existe, não ao fim da síntese.
- **RNF-35** — o prazo de síntese vale **até o primeiro byte**, e é por motor. Revoga o prazo total
  de RNF-20, que com streaming mataria a fala no meio.
- **RNF-22 revisto** — `SourceDataLine` no lugar de `Clip`; a obrigação de não deixar linha aberta
  continua igual.
- **DEF-10** — **vozes Piper `low`/`x_low` tocam 38% aceleradas e com o tom alterado**, porque o
  sample rate está fixo em 22050 no código e as vozes de 16000 Hz são 40 das 173 do índice oficial,
  incluindo `pt_BR-edresson-low`. Não observado em uso porque a voz do autor é `medium`.
- **Achado 37** — `kokoro-onnx` não publica CLI; a integração é `python -c`, custo fixo ~1,60 s.
- **Achado 38** — o default de threads do onnxruntime deixa o Kokoro mais lento que tempo real
  (RTF 1,56); `intra_op_num_threads=4` derruba para 0,37. **65,35 s → 15,45 s** no mesmo texto.
- **Achado 39** — **o `Clip` era o gargalo, não o motor.** Os dois motores sempre emitiram PCM
  incrementalmente. Tempo até a primeira palavra é quase constante no tamanho do texto: Piper
  ~2,1 s, Kokoro **2,41–2,56 s** com a rampa, contra 17,71 s do desenho de bloco único.
- **Achado 40** — **o tempo até falar depende só da primeira frase, não do trecho.** Custo fixo de
  1,50 s (dos quais 1,05 s são a carga do modelo) mais uma síntese linear no tamanho do primeiro
  pedaço (0,65 s a cada 30 chars). Um teto de palavras só no primeiro pedaço leva 1,81 s → 0,72 s;
  **o número de pedaços não afeta o tempo total** (16 contra 5 medem igual), o que descarta a
  rampa progressiva que a primeira versão deste plano propunha.
- **Achado 41** — cortar o texto **estica a fala**: 16,1 s numa tacada só contra 17,8 s cortado
  (+10%), porque a prosódia reinicia a cada pedaço. Aplicar o prefixo `" . "` só no primeiro pedaço
  devolve 0,6 s desses.
- **D-45** — 0,25x removido; migração de graça pelo `effectiveSpeechSpeed()`.
- **D-46** — vozes lidas da fonte nos dois motores; 0,40–2,20 ms medidos no ZIP do Kokoro.
- **D-47** — script Python embutido via `-c`, sem arquivo em `resources` e sem CLI próprio.
- **D-48** — **daemon avaliado e recusado**, com números: 1,60 s comprados por ~500 MB residentes
  e 961 MB de pico; ganho de 1,1× no texto longo.
- **D-49** — sidecar do Piper lido por regex de dois campos: não há parser JSON na plataforma, e a
  alternativa era uma dependência nova para ler um inteiro.
- **D-50** — **modelo int8 medido e recusado.** 2,2× menos memória e 0,4 s a menos de carga, ao
  preço de **4,5× menos velocidade** (RTF 1,55 contra 0,34), reproduzido nas três configurações de
  thread. Acima de 1,0 o motor fica mais lento que a reprodução e o streaming engasga. Fica o f32.
- **D-51** — **teto de palavras no primeiro pedaço, e só nele.** A rampa progressiva foi medida
  contra ele e empatou (0,73 s contra 0,72 s), com mais código — descartada. O prefixo `" . "`
  passa a valer só no primeiro pedaço, pelo Achado 41.
- **D-52** — **cache de grafo otimizado do ONNX recusado**: 0,2 s por mais 325 MB em disco e um
  artefato que o próprio onnxruntime marca como específico do hardware. A carga é I/O, não
  otimização.
- Atualizar **RNF-19** (a classe única agora cobre dois motores e a reprodução em streaming) e a
  seção "Piper TTS no ambiente" (linha 447) com o ambiente do Kokoro.

**`plans/sdd/HANDOFF.md`:** entrada de log com as medições de RTF, de primeiro áudio e do int8, o
estado dos testes e os roteiros T-3.70 a T-3.79 pendentes de IDE real.

Este plano também vai para `plans/` no repositório, ao lado do `sdd/`.

---

## Git

```bash
git checkout -b feature/kokoro-onnx-tts
```

Commits em Conventional Commits, na ordem de implementação:

1. `feat(tts): add TtsEngine setting with Kokoro-ONNX fields`
2. `feat(tts): list Kokoro and Piper voices from their own sources`
3. `fix(tts): read Piper sample rate from the voice sidecar`
4. `refactor(tts): rename ClaudePiperPlayback to ClaudeTtsPlayback with TtsCommand`
5. `feat(tts): stream audio through SourceDataLine instead of buffering a Clip`
6. `feat(tts): synthesize through Kokoro-ONNX via embedded Python script`
7. `feat(settings): engine selector and voice pickers for both engines`
8. `feat(tts): drop 0.25x speed unsupported by Kokoro`
9. `test(tts): cover streaming, command building, voice listing and speed migration`
10. `docs(sdd): register v1.11 — Kokoro-ONNX engine, streaming playback and DEF-10`

Os comandos ficam para você rodar — nada de `git` executado por mim.
