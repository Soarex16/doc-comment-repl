package com.github.soarex16.doccommentrepl.execution

import com.github.soarex16.doccommentrepl.markers.REPL_OUTPUT_MARKER
import com.github.soarex16.doccommentrepl.markers.parseCodeLines
import com.github.soarex16.doccommentrepl.markers.tryParseNextEolComments
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.util.text.nullize
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.console.PyExecuteConsoleCustomizer
import com.jetbrains.python.console.PydevConsoleRunner
import com.jetbrains.python.console.PythonConsoleRunnerFactory
import com.jetbrains.python.console.pydev.ConsoleCommunication
import com.jetbrains.python.console.pydev.ConsoleCommunicationListener

class PythonExecutor : SnippetExecutor {
    override fun formatComment(execResult: String) =
        "\"\"\"" + execResult.lines().joinToString("\n") { REPL_OUTPUT_MARKER + it } + "\"\"\""

    override fun parseSnippet(element: PsiElement) = when (element.elementType) {
        PyTokenTypes.END_OF_LINE_COMMENT -> tryParseNextEolComments(element)
        PyTokenTypes.DOCSTRING, PyTokenTypes.TRIPLE_QUOTED_STRING -> parseCodeLines(element)
        else -> null
    }

    override fun executeSnippet(ctx: ExecutionContext) {
        val runner: PydevConsoleRunner = PythonConsoleRunnerFactory.getInstance().createConsoleRunner(ctx.project, null)

        runner.addConsoleListener { view ->
            runner.pydevConsoleCommunication.addCommunicationListener(object : ConsoleCommunicationListener {
                override fun commandExecuted(more: Boolean) {
                    WriteCommandAction.runWriteCommandAction(ctx.project) {
                        val mockOutput = "896"
                        ctx.onSnippetExecuted(mockOutput)
                        //val output = runner.consoleView.getHistoryViewer().getDocument().getText()
                        val output = view.file.text
                        ctx.onSnippetExecuted(output)
                        //val output = view.file.text
                        /*val commentString = output.lines().joinToString { "\n#$it" }
                        activeDocument.insertString(psiElement.textRange.endOffset, commentString)*/
                    }
                }

                override fun inputRequested() {}
            })
            val output = null /*view.file.text
            ctx.onSnippetExecuted(output)*/
        }

        runner.run(true)
        runner.consoleView.executeCode(ctx.code, null)
    }
}