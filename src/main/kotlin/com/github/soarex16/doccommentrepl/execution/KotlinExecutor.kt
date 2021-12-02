package com.github.soarex16.doccommentrepl.execution

import com.github.soarex16.doccommentrepl.logError
import com.github.soarex16.doccommentrepl.markers.REPL_OUTPUT_MARKER
import com.github.soarex16.doccommentrepl.markers.parseCodeLines
import com.github.soarex16.doccommentrepl.markers.tryParseNextEolComments
import com.github.soarex16.doccommentrepl.repl.console.KotlinConsoleKeeper
import com.github.soarex16.doccommentrepl.repl.console.KotlinConsoleRunner
import com.intellij.execution.process.BaseOSProcessHandler
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.nextLeaf
import org.jetbrains.kotlin.cli.common.repl.replInputAsXml
import org.jetbrains.kotlin.lexer.KtTokens

class KotlinExecutor : SnippetExecutor {
    override fun formatComment(execResult: String) =
        "/*" + execResult.lines().joinToString("\n") { REPL_OUTPUT_MARKER + it } + "*/"

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

    override fun executeSnippet(ctx: ExecutionContext) {
        val keeper = KotlinConsoleKeeper.getInstance(ctx.project)
        val runner: KotlinConsoleRunner
        if (keeper.currentRunner == null) {
            runner = keeper.run(ctx.module, previousCompilationFailed = false)
        } else {
            runner = keeper.currentRunner!!
        }
        runner.onExecutedCallback = ctx.onSnippetExecuted

        sendCommandToProcess(ctx.code, runner)
    }

    private fun sendCommandToProcess(command: String, runner: KotlinConsoleRunner) {
        val processHandler = runner.processHandler
        val processInputOS =
            processHandler.processInput ?: return this.logError("<p>Broken process stream</p>")
        val charset = (processHandler as? BaseOSProcessHandler)?.charset ?: Charsets.UTF_8

        val xmlRes = command.replInputAsXml()

        val bytes = ("$xmlRes\n").toByteArray(charset)
        processInputOS.write(bytes)
        processInputOS.flush()
    }
}