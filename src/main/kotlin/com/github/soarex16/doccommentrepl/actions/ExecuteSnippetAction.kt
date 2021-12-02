package com.github.soarex16.doccommentrepl.actions

import com.github.soarex16.doccommentrepl.errorNotification
import com.github.soarex16.doccommentrepl.logError
import com.github.soarex16.doccommentrepl.repl.console.KotlinConsoleKeeper
import com.github.soarex16.doccommentrepl.repl.console.KotlinConsoleRunner
import com.intellij.execution.process.BaseOSProcessHandler
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.jetbrains.python.actions.PyExecuteInConsole
import com.jetbrains.python.console.PyExecuteConsoleCustomizer
import org.jetbrains.kotlin.KotlinIdeaReplBundle
import org.jetbrains.kotlin.cli.common.repl.replInputAsXml

class ExecuteSnippetAction(
    val code: String,
    private val callElement: SmartPsiElementPointer<PsiElement>?,
    val snippetTextRange: TextRange
) : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val psiElement = callElement?.element ?: return

        val project = event.project
            ?: return errorNotification(null, KotlinIdeaReplBundle.message("cannot.find.project"))
        val activeDocument = FileEditorManager.getInstance(project).selectedTextEditor?.document
            ?: return errorNotification(null, KotlinIdeaReplBundle.message("no.modules.were.found"))
        val activeDocFile = FileDocumentManager.getInstance().getFile(activeDocument)

        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(activeDocFile!!)!!

        when (psiElement.language.id) {
            "kotlin" -> executeKotlinSnippet(project, module, activeDocument)
            "Python" -> executePythonSnippet(event, project, module, activeDocument)
        }
    }

    private fun executePythonSnippet(event: AnActionEvent, project: Project, module: Module, activeDocument: Document) {
        val config = PyExecuteConsoleCustomizer.instance.getContextConfig(event.dataContext)
        val editor = event.getData(CommonDataKeys.EDITOR)
        PyExecuteInConsole
                .executeCodeInConsole(project, code, editor, true, true, false, config)
    }

    private fun executeKotlinSnippet(project: Project, module: Module, activeDocument: Document) {
        val keeper = KotlinConsoleKeeper.getInstance(project)
        val runner: KotlinConsoleRunner
        if (keeper.currentRunner == null) {
            runner = keeper.run(module, previousCompilationFailed = false)
        } else {
            runner = keeper.currentRunner!!
        }
        runner.activeDocument = activeDocument
        runner.callElementRef = callElement
        sendCommandToProcess(code, runner)
    }

    private fun sendCommandToProcess(command: String, runner: KotlinConsoleRunner) {
        val processHandler = runner.processHandler
        val processInputOS =
            processHandler.processInput ?: return logError(this::class.java, "<p>Broken process stream</p>")
        val charset = (processHandler as? BaseOSProcessHandler)?.charset ?: Charsets.UTF_8

        val xmlRes = command.replInputAsXml()

        val bytes = ("$xmlRes\n").toByteArray(charset)
        processInputOS.write(bytes)
        processInputOS.flush()
    }
}