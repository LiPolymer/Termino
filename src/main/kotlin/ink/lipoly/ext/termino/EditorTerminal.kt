package ink.lipoly.ext.termino

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerKeys
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab
import com.intellij.ui.content.Content
import java.beans.PropertyChangeListener
import javax.swing.Icon
import javax.swing.JComponent

internal class EditorTerminalFile(val tab: TerminalToolWindowTab) :
    LightVirtualFile(tab.content.displayName?.takeUnless(String::isBlank) ?: "Terminal") {
    init {
        isWritable = true
        putUserData(FileEditorManagerKeys.FORBID_TAB_SPLIT, true)
    }
}

class EditorTerminalIconProvider : FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? =
        if (file is EditorTerminalFile) TerminoSettings.getInstance().toolbarIcon() else null
}

class EditorTerminalProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file is EditorTerminalFile

    override fun acceptRequiresReadAction(): Boolean = false

    override fun createEditor(project: Project, file: VirtualFile): FileEditor = EditorTerminal(project, file as EditorTerminalFile)

    override fun getEditorTypeId(): String = "termino-terminal-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

private class EditorTerminal(private val project: Project, private val file: EditorTerminalFile) : UserDataHolderBase(), FileEditor {
    private var disposed = false
    private val tab = file.tab
    private val displayNameListener = PropertyChangeListener { event ->
        if (event.propertyName == Content.PROP_DISPLAY_NAME) syncDisplayName()
    }

    init {
        tab.view.title.change { trackTerminalApplicationTitleChanges = true }
        tab.content.addPropertyChangeListener(displayNameListener)
        syncDisplayName()
    }

    private fun syncDisplayName() {
        ApplicationManager.getApplication().invokeLater({
            if (!disposed && !project.isDisposed) {
                val displayName = tab.content.displayName
                if (!displayName.isNullOrBlank() && file.name != displayName) {
                    file.rename(null, displayName)
                    FileEditorManager.getInstance(project).updateFilePresentation(file)
                }
            }
        }, project.disposed)
    }

    override fun getComponent(): JComponent = tab.content.component

    override fun getPreferredFocusedComponent(): JComponent = tab.content.preferredFocusableComponent

    override fun getName(): String = file.name

    override fun getFile(): VirtualFile = file

    override fun setState(state: FileEditorState) = Unit

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = !disposed

    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun dispose() {
        disposed = true
        tab.content.removePropertyChangeListener(displayNameListener)
        if (file.getUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN) != true) {
            Disposer.dispose(tab.content)
        }
    }
}
