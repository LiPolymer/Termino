package ink.lipoly.ext.termino

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Disposer
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager

class OpenEditorTerminalAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
        event.presentation.icon = TerminoSettings.getInstance().toolbarIcon()
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val tab = TerminalToolWindowTabsManager.getInstance(project)
            .createTabBuilder()
            .workingDirectory(project.basePath)
            .shouldAddToToolWindow(false)
            .createTab()
        try {
            val command = TerminoSettings.getInstance().state.startupCommand
            if (command.isNotBlank()) tab.view.createSendTextBuilder().shouldExecute().send(command)
            FileEditorManager.getInstance(project).openFile(EditorTerminalFile(tab), true)
        } catch (failure: Throwable) {
            Disposer.dispose(tab.content)
            throw failure
        }
    }
}
