package com.github.soarex16.doccommentrepl.execution

import com.github.soarex16.doccommentrepl.markers.parseCodeLines
import com.github.soarex16.doccommentrepl.markers.tryParseNextEolComments
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.elementType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.actions.PyExecuteInConsole
import com.jetbrains.python.console.PyExecuteConsoleCustomizer

class PythonExecutor: SnippetExecutor {
    override fun formatComment(execResult: String) = execResult.lines().joinToString("\n") { "#$it" }

    override fun parseSnippet(element: PsiElement) = when (element.elementType) {
        PyTokenTypes.END_OF_LINE_COMMENT -> tryParseNextEolComments(element)
        PyTokenTypes.DOCSTRING, PyTokenTypes.TRIPLE_QUOTED_STRING -> parseCodeLines(element)
        else -> null
    }

    override fun executeSnippet(event: AnActionEvent, callElement: SmartPsiElementPointer<PsiElement>, code: String, project: Project, module: Module, activeDocument: Document): String? {
        // TODO: сохранять модули, подтягивать зависимости, спрятать консоль, редиректнуть вывод

        val config = PyExecuteConsoleCustomizer.instance.getContextConfig(event.dataContext)
        val editor = event.getData(CommonDataKeys.EDITOR)
        PyExecuteInConsole
                .executeCodeInConsole(project, code, editor, true, true, false, config)

        return null
    }
}