package com.github.soarex16.doccommentrepl

import com.github.soarex16.doccommentrepl.repl.console.KotlinConsoleKeeper
import com.github.soarex16.doccommentrepl.repl.console.KotlinConsoleRunner
import com.intellij.execution.process.BaseOSProcessHandler
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.KotlinIdeaReplBundle
import org.jetbrains.kotlin.cli.common.repl.replInputAsXml
import org.jetbrains.kotlin.console.actions.errorNotification
import org.jetbrains.kotlin.console.actions.logError

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