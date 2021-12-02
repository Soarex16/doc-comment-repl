package com.github.soarex16.doccommentrepl.execution

import com.github.soarex16.doccommentrepl.markers.REPL_OUTPUT_MARKER
import com.github.soarex16.doccommentrepl.markers.parseCodeLines
import com.github.soarex16.doccommentrepl.markers.tryParseNextEolComments
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.console.*
import com.jetbrains.python.console.PyExecuteConsoleCustomizer
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
        val runner: PydevConsoleRunner = PythonConsoleRunnerFactory
            .getInstance()
            .createConsoleRunner(ctx.project, ctx.module)

        runner.addConsoleListener{ view ->
            runner.pydevConsoleCommunication.addCommunicationListener(object : ConsoleCommunicationListener {
                override fun commandExecuted(more: Boolean) {
                    Thread.sleep(1500)
                    val pythonPrompt = "..."
                    val output = view.historyViewer.document.text
                    val lastPromptIndex = output.lastIndexOf(pythonPrompt)

                    if (lastPromptIndex == -1)
                        return

                    val result = output.substring(lastPromptIndex + pythonPrompt.length)
                    if (result.isBlank())
                        return

                    runner.pydevConsoleCommunication.close()

                    ctx.onSnippetExecuted(result)
                }

                override fun inputRequested() { }
            })
            runner.consoleView.executeInConsole(ctx.code)
        }

        runner.run(true)
    }
}