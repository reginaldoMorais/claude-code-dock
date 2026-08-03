package dev.reginaldomorais.claudedock.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

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

            group("Piper TTS") {
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
            }
        }
    }
}
