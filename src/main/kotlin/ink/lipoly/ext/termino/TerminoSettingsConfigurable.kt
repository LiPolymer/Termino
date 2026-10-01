package ink.lipoly.ext.termino

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.IconDeferrer
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel

class TerminoSettingsConfigurable : Configurable {
    private var panel: JComponent? = null
    private var commandField: JBTextField? = null
    private var iconCombo: JComboBox<ButtonIcon>? = null
    private var pathField: JBTextField? = null
    private var browseButton: JButton? = null
    private var preview: JLabel? = null

    override fun getDisplayName(): String = "Termino"

    override fun createComponent(): JComponent {
        panel?.let { return it }
        val command = JBTextField().apply { columns = 36 }
        val icon = JComboBox(ButtonIcon.entries.toTypedArray()).apply {
            preferredSize = Dimension(JBUI.scale(160), preferredSize.height)
            renderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean,
                ): JComponent {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    if (value is ButtonIcon) {
                        text = when (value) {
                            ButtonIcon.TERMINAL -> "终端"
                            ButtonIcon.PLAY -> "运行"
                            ButtonIcon.OH_MY_PI_GRADIENT -> "Oh My Pi（渐变）"
                            ButtonIcon.OH_MY_PI_MONO -> "Oh My Pi（单色）"
                            ButtonIcon.CUSTOM -> "自定义图标"
                        }
                        icon = if (value == ButtonIcon.CUSTOM) null else TerminoButtonIcons.forOption(value)
                    }
                    return this
                }
            }
        }
        val path = JBTextField().apply { isEditable = false }
        val browse = JButton("浏览…").apply {
            addActionListener {
                val descriptor = FileChooserDescriptorFactory.singleFile()
                    .withTitle("选择工具栏图标")
                    .withExtensionFilter("图标文件", "svg", "png")
                val selected = FileChooser.chooseFile(descriptor, this, null, null) ?: return@addActionListener
                path.text = selected.path
                icon.selectedItem = ButtonIcon.CUSTOM
                updatePreview()
            }
        }
        val iconPreview = JLabel()
        icon.addActionListener {
            val custom = icon.selectedItem == ButtonIcon.CUSTOM
            path.isEnabled = custom
            browse.isEnabled = custom
            updatePreview()
        }
        commandField = command
        iconCombo = icon
        pathField = path
        browseButton = browse
        preview = iconPreview
        val choiceRow = JPanel(BorderLayout(JBUI.scale(12), 0)).apply {
            add(icon, BorderLayout.WEST)
            add(iconPreview, BorderLayout.CENTER)
        }
        val pathRow = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            add(path, BorderLayout.CENTER)
            add(browse, BorderLayout.EAST)
        }
        return FormBuilder.createFormBuilder()
            .addComponent(JLabel("终端会话").apply { font = font.deriveFont(java.awt.Font.BOLD) })
            .addLabeledComponent("启动指令:", command)
            .addTooltip("新建终端时执行一次；留空则不执行。")
            .addVerticalGap(JBUI.scale(16))
            .addComponent(JLabel("主工具栏按钮").apply { font = font.deriveFont(java.awt.Font.BOLD) })
            .addLabeledComponent("图标:", choiceRow)
            .addLabeledComponent("自定义文件:", pathRow)
            .addTooltip("选择本地 SVG 或 PNG 图片；推荐 16 × 16。设置在整个 IDE 中生效。")
            .panel.let { form ->
                JPanel(BorderLayout()).apply {
                    border = JBUI.Borders.empty(16)
                    add(form, BorderLayout.NORTH)
                }
            }
            .also { panel = it; reset() }
    }

    private fun updatePreview() {
        val label = preview ?: return
        val selected = iconCombo?.selectedItem as? ButtonIcon ?: return
        if (selected == ButtonIcon.CUSTOM) {
            val path = pathField?.text.orEmpty()
            try {
                label.icon = TerminoButtonIcons.loadCustom(path)
                label.text = ""
            } catch (error: Exception) {
                label.icon = null
                label.text = if (path.isEmpty()) "请选择图片" else error.message.orEmpty()
            }
        } else {
            label.icon = TerminoButtonIcons.forOption(selected)
            label.text = ""
        }
    }

    override fun isModified(): Boolean {
        val command = commandField ?: return false
        val icon = iconCombo ?: return false
        val saved = TerminoSettings.getInstance().state
        return command.text != saved.startupCommand ||
            icon.selectedItem != saved.buttonIcon ||
            pathField?.text != saved.customIconPath
    }

    override fun apply() {
        val command = commandField ?: return
        val icon = iconCombo ?: return
        try {
            TerminoSettings.getInstance().update(command.text, icon.selectedItem as ButtonIcon, pathField?.text.orEmpty())
        } catch (error: Exception) {
            throw ConfigurationException("无法使用自定义图标：${error.message}")
        }
        // File icon cache keys do not include plugin settings.
        IconDeferrer.getInstance().clearCache()
        ApplicationManager.getApplication().invokeLater {
            for (project in ProjectManager.getInstance().openProjects) {
                if (project.isDisposed) continue
                val editorManager = FileEditorManager.getInstance(project)
                for (file in editorManager.openFiles) {
                    if (file is EditorTerminalFile) editorManager.updateFilePresentation(file)
                }
            }
        }
    }

    override fun reset() {
        val saved = TerminoSettings.getInstance().state
        commandField?.text = saved.startupCommand
        pathField?.text = saved.customIconPath
        iconCombo?.selectedItem = saved.buttonIcon
        val custom = saved.buttonIcon == ButtonIcon.CUSTOM
        pathField?.isEnabled = custom
        browseButton?.isEnabled = custom
        updatePreview()
    }

    override fun disposeUIResources() {
        panel = null
        commandField = null
        iconCombo = null
        pathField = null
        browseButton = null
        preview = null
    }
}
