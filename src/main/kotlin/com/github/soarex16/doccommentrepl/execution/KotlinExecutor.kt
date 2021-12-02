package com.github.soarex16.doccommentrepl.execution

import com.github.soarex16.doccommentrepl.logError
import com.github.soarex16.doccommentrepl.markers.parseCodeLines
import com.github.soarex16.doccommentrepl.markers.tryParseNextEolComments
import com.github.soarex16.doccommentrepl.repl.console.KotlinConsoleKeeper
import com.github.soarex16.doccommentrepl.repl.console.KotlinConsoleRunner
import com.intellij.execution.process.BaseOSProcessHandler
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.cli.common.repl.replInputAsXml
import org.jetbrains.kotlin.lexer.KtTokens

class KotlinExecutor: SnippetExecutor {
    override fun formatComment(execResult: String) = execResult.lines().joinToString("\n") { "//$it" }

    /*
        NOTE: не учтен случай, когда в многострочном комментарии (BLOCK_COMMENT, DOC_COMMENT)
        несколько сниппетов. Например:

        >>> 1 + 2
        ...some text
        >>> 2 + 3

     */
    override fun parseSnippet(element: PsiElement) = when (element.elementType) {
        KtTokens.DOC_COMMENT, KtTokens.BLOCK_COMMENT -> parseCodeLines(element)
        KtTokens.EOL_COMMENT -> tryParseNextEolComments(element)
        else -> null
    }


    override fun executeSnippet(event: AnActionEvent, callElement: SmartPsiElementPointer<PsiElement>, code: String, project: Project, module: Module, activeDocument: Document): String? {
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

        return null
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