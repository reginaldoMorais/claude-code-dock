package dev.reginaldomorais.claudedock.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Tela em Settings > Tools > Claude Code Dock (RF-10, RF-19).
 *
 * É `projectConfigurable` porque o `CLAUDE_CONFIG_DIR` vale por projeto — em IDEs JetBrains,
 * uma janela aberta é um projeto. O caminho do executável continua sendo de aplicação:
 * o mesmo binário serve a todos os projetos.
 */
class ClaudeDockConfigurable(private val project: Project) : Configurable {

    private var executableField: TextFieldWithBrowseButton? = null
    private var configDirField: TextFieldWithBrowseButton? = null

    override fun getDisplayName(): String = "Claude Code Dock"

    override fun createComponent(): JComponent {
        val executableDescriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
            .withTitle("Executável do Claude Code")
            .withDescription("Selecione o binário do Claude Code")

        val executable = TextFieldWithBrowseButton().apply {
            addBrowseFolderListener(null, executableDescriptor)
        }
        executableField = executable

        val configDirDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle("Diretório de configuração do Claude Code")
            .withDescription("Selecione o diretório usado como CLAUDE_CONFIG_DIR")

        val configDir = TextFieldWithBrowseButton().apply {
            addBrowseFolderListener(null, configDirDescriptor)
        }
        configDirField = configDir

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Executável do Claude Code:", executable, true)
            .addComponentToRightColumn(
                JBLabel(
                    "Vale para todos os projetos. Deixe \"claude\" para resolver pelo PATH.",
                    UIUtil.ComponentStyle.SMALL,
                ),
            )
            .addLabeledComponent("CLAUDE_CONFIG_DIR:", configDir, true)
            .addComponentToRightColumn(
                JBLabel(
                    "Somente este projeto. Vazio usa o padrão do CLI (~/.claude). " +
                        "Aplica-se às próximas sessões abertas.",
                    UIUtil.ComponentStyle.SMALL,
                ),
            )
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean =
        executableField?.text?.trim() != ClaudeDockSettings.getInstance().claudeExecutable ||
            configDirField?.text?.trim() != projectSettings().claudeConfigDir

    override fun apply() {
        val executable = executableField?.text?.trim().orEmpty()
        ClaudeDockSettings.getInstance().claudeExecutable =
            executable.ifEmpty { ClaudeDockSettings.DEFAULT_EXECUTABLE }

        projectSettings().claudeConfigDir = configDirField?.text?.trim().orEmpty()
        reset()
    }

    override fun reset() {
        executableField?.text = ClaudeDockSettings.getInstance().claudeExecutable
        configDirField?.text = projectSettings().claudeConfigDir
    }

    override fun disposeUIResources() {
        executableField = null
        configDirField = null
    }

    private fun projectSettings(): ClaudeDockProjectSettings =
        ClaudeDockProjectSettings.getInstance(project)
}
