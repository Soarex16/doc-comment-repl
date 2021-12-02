package com.github.soarex16.doccommentrepl

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.python.console.*
import com.jetbrains.python.run.PythonRunConfiguration

object PyExecuteInConsole {
    @JvmStatic
    fun executeCodeInConsole(project: Project,
                             commandText: String?,
                             editor: Editor?,
                             canUseExistingConsole: Boolean,
                             canUseDebugConsole: Boolean,
                             requestFocusToConsole: Boolean,
                             config: PythonRunConfiguration?): PydevConsoleRunner? {
        var existingConsole: RunContentDescriptor? = null
        var newConsoleListener: PydevConsoleRunner.ConsoleListener? = null
        val virtualFile = (editor as? EditorImpl)?.virtualFile
        if (canUseExistingConsole) {
            if (virtualFile != null && PyExecuteConsoleCustomizer.instance.isCustomDescriptorSupported(virtualFile)) {
                val (descriptor, listener) = getCustomDescriptor(project, editor)
                existingConsole = descriptor
                newConsoleListener = listener
            }
            else {
                existingConsole = getSelectedPythonConsole(project)
            }
        }

        var runner: PydevConsoleRunner? = null

        if (existingConsole != null) {
            val console = existingConsole.executionConsole
            runner = console as PydevConsoleRunner
            (console as PyCodeExecutor).executeCode(commandText, editor)

            //val consoleView = showConsole(project, existingConsole)
        } else {
            if (!PyExecuteConsoleCustomizer.instance.isConsoleStarting(virtualFile, commandText)) {
                runner = startNewConsoleInstance(project, virtualFile, commandText, config, newConsoleListener)
            }
        }

        return runner
    }

    private fun getCustomDescriptor(project: Project, editor: Editor?): Pair<RunContentDescriptor?, PydevConsoleRunner.ConsoleListener?> {
        val virtualFile = (editor as? EditorImpl)?.virtualFile ?: return Pair(null, null)
        val executeCustomizer = PyExecuteConsoleCustomizer.instance
        when (executeCustomizer.getCustomDescriptorType(virtualFile)) {
            DescriptorType.NEW -> {
                return Pair(null, createNewConsoleListener(project, virtualFile))
            }
            DescriptorType.EXISTING -> {
                val console = executeCustomizer.getExistingDescriptor(virtualFile)
                if (console != null && isAlive(console)) {
                    return Pair(console, null)
                }
                else {
                    return Pair(null, createNewConsoleListener(project, virtualFile))
                }
            }
            DescriptorType.STARTING -> {
                return Pair(null, null)
            }
            DescriptorType.NON_INTERACTIVE -> {
                throw IllegalStateException("This code shouldn't be called for a non-interactive descriptor")
            }
            else -> {
                throw IllegalStateException("Custom descriptor for ${virtualFile} is null")
            }
        }
    }

    fun createNewConsoleListener(project: Project, virtualFile: VirtualFile): PydevConsoleRunner.ConsoleListener {
        return PydevConsoleRunner.ConsoleListener { consoleView ->
            val consoles = getAllRunningConsoles(project)
            val newDescriptor = consoles.find { it.executionConsole === consoleView }
            PyExecuteConsoleCustomizer.instance.updateDescriptor(virtualFile, DescriptorType.EXISTING, newDescriptor)
        }
    }

    fun getAllRunningConsoles(project: Project?): List<RunContentDescriptor> {
        val toolWindow = PythonConsoleToolWindow.getInstance(project!!)
        return if (toolWindow != null && toolWindow.isInitialized) {
            toolWindow.consoleContentDescriptors.filter { isAlive(it) }
        }
        else emptyList()
    }

    private fun getSelectedPythonConsole(project: Project): RunContentDescriptor? {
        val toolWindow = PythonConsoleToolWindow.getInstance(project) ?: return null
        if (!toolWindow.isInitialized) return null
        val consoles = toolWindow.consoleContentDescriptors.filter { isAlive(it) }
        return consoles.singleOrNull()
                ?: toolWindow.selectedContentDescriptor.takeIf { it in consoles }
                ?: consoles.firstOrNull()
    }

    fun isAlive(dom: RunContentDescriptor): Boolean {
        val processHandler = dom.processHandler
        return processHandler != null && !processHandler.isProcessTerminated
    }

    private fun startNewConsoleInstance(project: Project,
                                        virtualFile: VirtualFile?,
                                        runFileText: String?,
                                        config: PythonRunConfiguration?,
                                        listener: PydevConsoleRunner.ConsoleListener?): PydevConsoleRunner {
        val consoleRunnerFactory = PythonConsoleRunnerFactory.getInstance()
        val runner = if (runFileText == null || config == null) {
            consoleRunnerFactory.createConsoleRunner(project, null)
        }
        else {
            consoleRunnerFactory.createConsoleRunnerWithFile(project, null, runFileText, config)
        }

        runner.addConsoleListener { consoleView ->
            if (consoleView is PyCodeExecutor) {
                (consoleView as PyCodeExecutor).executeCode(runFileText, null)
                PythonConsoleToolWindow.getInstance(project)?.toolWindow?.show(null)
            }
        }
        if (listener != null) {
            runner.addConsoleListener(listener)
        }
        virtualFile?.let {
            PyExecuteConsoleCustomizer.instance.notifyRunnerStart(it, runner)
        }

        runner.run(false)
        return runner
    }

    private fun showConsole(project: Project,
                            descriptor: RunContentDescriptor): PythonConsoleView? {
        PythonConsoleToolWindow.getInstance(project)?.toolWindow?.let { toolWindow ->
            if (!toolWindow.isVisible) {
                toolWindow.show(null)
            }
            val contentManager = toolWindow.contentManager
            contentManager.findContent(PyExecuteConsoleCustomizer.instance.getDescriptorName(descriptor))?.let {
                contentManager.setSelectedContent(it)
            }
        }
        return descriptor.executionConsole as? PythonConsoleView
    }
}