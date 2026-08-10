package dev.reginaldomorais.claudedock.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.selected

/**
 * Tela em Settings > Tools > Claude Code Dock (RF-10, RF-19).
 *
 * É `projectConfigurable` porque o `CLAUDE_CONFIG_DIR` vale por projeto — em IDEs JetBrains,
 * uma janela aberta é um projeto. O caminho do executável continua sendo de aplicação:
 * o mesmo binário serve a todos os projetos.
 *
 * `BoundConfigurable` com o Kotlin UI DSL entrega os títulos de seção, os separadores e o
 * alinhamento dos campos prontos, e dispensa `isModified`/`apply`/`reset` escritos à mão —
 * as ligações abaixo é que definem o que é lido e gravado.
 */
class ClaudeDockConfigurable(private val project: Project) :
    BoundConfigurable("Claude Code Dock") {

    override fun createPanel(): DialogPanel {
        val settings = ClaudeDockSettings.getInstance()
        val projectSettings = ClaudeDockProjectSettings.getInstance(project)

        return panel {
            group("Executável") {
                row("Comando do Claude Code:") {
                    textFieldWithBrowseButton(
                        FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                            .withTitle("Executável do Claude Code")
                            .withDescription("Selecione o binário do Claude Code"),
                        project,
                    )
                        .align(AlignX.FILL)
                        .bindText(settings::claudeExecutable)
                        .comment(
                            "Vale para todos os projetos. " +
                                "Deixe \"claude\" para resolver pelo PATH.",
                        )
                }
            }

            group("Configuração do CLI") {
                row("CLAUDE_CONFIG_DIR:") {
                    textFieldWithBrowseButton(
                        FileChooserDescriptorFactory.createSingleFolderDescriptor()
                            .withTitle("Diretório de configuração do Claude Code")
                            .withDescription("Selecione o diretório usado como CLAUDE_CONFIG_DIR"),
                        project,
                    )
                        .align(AlignX.FILL)
                        .bindText(projectSettings::claudeConfigDir)
                        .comment(
                            "Somente este projeto. Vazio usa o padrão do CLI (~/.claude). " +
                                "Aplica-se às próximas sessões abertas.",
                        )
                }
            }

            group("Aparência") {
                row("Respiro nas bordas:") {
                    spinner(ClaudeDockSettings.MIN_PADDING..ClaudeDockSettings.MAX_PADDING)
                        .bindIntValue(settings::sessionPadding)
                        .comment(
                            "Em pixels. Vale para todos os projetos. Distância entre o conteúdo " +
                                "da sessão e as bordas da janela. " +
                                "Aplica-se às próximas sessões abertas.",
                        )
                }
            }

            group("Voz (TTS)") {
                lateinit var piperSelected: Cell<JBRadioButton>
                lateinit var kokoroSelected: Cell<JBRadioButton>

                buttonsGroup {
                    row("Motor:") {
                        // Kokoro primeiro: é o padrão, e a ordem da tela é o que sinaliza isso.
                        kokoroSelected = radioButton("Kokoro-ONNX", TtsEngine.KOKORO)
                        piperSelected = radioButton("Piper", TtsEngine.PIPER)
                    }
                }.bind(
                    object : MutableProperty<TtsEngine> {
                        override fun get(): TtsEngine = settings.ttsEngine
                        override fun set(value: TtsEngine) { settings.ttsEngine = value }
                    },
                    TtsEngine::class.java,
                )

                // Os dois blocos abaixo se alternam pelo motor escolhido: mostrar campos de um
                // motor que não está em uso só convida a configurar a coisa errada.
                rowsRange {
                    row("Executável do Piper:") {
                        textFieldWithBrowseButton(
                            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                                .withTitle("Executável do Piper")
                                .withDescription("Selecione o binário do Piper TTS"),
                            project,
                        )
                            .align(AlignX.FILL)
                            .bindText(settings::piperExecutable)
                            .comment(
                                "Piper é um TTS local open-source. Veja https://github.com/OHF-Voice/piper1-gpl. " +
                                    "Deixe \"piper\" para resolver pelo PATH.",
                            )
                    }

                    row("Modelo de voz (.onnx):") {
                        textFieldWithBrowseButton(
                            FileChooserDescriptorFactory.createSingleFileDescriptor("onnx")
                                .withTitle("Arquivo de modelo do Piper")
                                .withDescription("Selecione o arquivo .onnx da voz desejada"),
                            project,
                        )
                            .align(AlignX.FILL)
                            .bindText(settings::piperModel)
                            .comment(
                                "Caminho absoluto para a voz desejada. Obrigatório para ativar síntese. " +
                                    "Vale para todos os projetos.",
                            )
                    }

                    row("Voz do Piper:") {
                        val models = PiperVoices.listModels(settings.effectivePiperModel())
                        comboBox(models, textListCellRenderer("") { it?.let(PiperVoices::label) ?: "" })
                            .align(AlignX.FILL)
                            .bindItem(
                                { models.firstOrNull { m -> m.path == settings.effectivePiperModel() } },
                                { if (it != null) settings.piperModel = it.path },
                            )
                            .comment(
                                "Vozes instaladas na mesma pasta do modelo. A taxa de amostragem vem do " +
                                    "arquivo .onnx.json de cada voz. A lista é lida ao abrir esta tela.",
                            )
                    }
                }.visibleIf(piperSelected.selected)

                rowsRange {
                    row("Interpretador Python:") {
                        textFieldWithBrowseButton(
                            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                                .withTitle("Interpretador Python")
                                .withDescription("Selecione o python com kokoro-onnx instalado"),
                            project,
                        )
                            .align(AlignX.FILL)
                            .bindText(settings::kokoroPython)
                            .comment(
                                "Precisa ter os pacotes kokoro-onnx e onnxruntime. Veja " +
                                    "https://github.com/thewh1teagle/kokoro-onnx. Se o seu Python for um shim " +
                                    "(pyenv, asdf), aponte o caminho absoluto: o PATH do IDE pode não incluí-lo.",
                            )
                    }

                    row("Modelo (.onnx):") {
                        textFieldWithBrowseButton(
                            FileChooserDescriptorFactory.createSingleFileDescriptor("onnx")
                                .withTitle("Modelo do Kokoro")
                                .withDescription("Selecione o arquivo kokoro-v1.0.onnx"),
                            project,
                        )
                            .align(AlignX.FILL)
                            .bindText(settings::kokoroModel)
                            .comment(
                                "Use o modelo f32. O int8 é menor, porém foi medido 4,5x mais lento — " +
                                    "lento demais para a fala começar antes do fim da síntese.",
                            )
                    }

                    row("Arquivo de vozes:") {
                        textFieldWithBrowseButton(
                            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                                .withTitle("Vozes do Kokoro")
                                .withDescription("Selecione o arquivo voices-v1.0.bin"),
                            project,
                        )
                            .align(AlignX.FILL)
                            .bindText(settings::kokoroVoices)
                            .comment("Obrigatório para ativar síntese. Vale para todos os projetos.")
                    }

                    row("Voz do Kokoro:") {
                        val voices = KokoroVoices.listVoices(settings.effectiveKokoroVoices())
                        comboBox(voices, textListCellRenderer("") { KokoroVoices.label(it) })
                            .align(AlignX.FILL)
                            .bindItem(
                                {
                                    val current = settings.effectiveKokoroVoice()
                                    if (current in voices) current else voices.firstOrNull()
                                },
                                { settings.kokoroVoice = it ?: ClaudeDockSettings.DEFAULT_KOKORO_VOICE },
                            )
                            .comment(
                                "Lidas do próprio arquivo de vozes, agrupadas por idioma. As de português " +
                                    "são pf_dora, pm_alex e pm_santa. A lista é lida ao abrir esta tela.",
                            )
                    }
                }.visibleIf(kokoroSelected.selected)

                row("Velocidade da fala:") {
                    comboBox(
                        ClaudeDockSettings.SPEECH_SPEEDS.keys.toList(),
                        textListCellRenderer("") { ClaudeDockSettings.speechSpeedLabel(it) },
                    )
                        .bindItem(
                            { settings.effectiveSpeechSpeed() },
                            { settings.speechSpeed = it ?: ClaudeDockSettings.DEFAULT_SPEECH_SPEED },
                        )
                        .comment(
                            "Mesmos valores do submenu \"Velocidade\" no menu \"Áudio\". " +
                                "\"1x\" mantém o padrão do próprio modelo de voz. " +
                                "Vale para todos os projetos. Aplica-se à próxima fala.",
                        )
                }
            }
        }
    }
}
