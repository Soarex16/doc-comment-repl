package com.github.soarex16.doccommentrepl.markers

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.jetbrains.python.PyTokenTypes

private val applicablePythonElementTypes = setOf(
        PyTokenTypes.END_OF_LINE_COMMENT,
        PyTokenTypes.DOCSTRING,
        PyTokenTypes.TRIPLE_QUOTED_STRING
)

/**
 * Провайдер маркеров запуска сниппетов кода в Питоне
 */
class PythonSnippetRunnerLineMarkProvider : ExecuteSnippetRunnerLineMarkProvider() {
    override fun isApplicable(element: PsiElement): Boolean {
        return element.elementType in applicablePythonElementTypes && element.text.indexOf(SNIPPET_START_MARKER) != -1
    }

    override fun extractCodeSnippet(element: PsiElement): Pair<String, TextRange>? {
        return when (element.elementType) {
            PyTokenTypes.END_OF_LINE_COMMENT -> tryParseNextEolComments(element)
            PyTokenTypes.DOCSTRING, PyTokenTypes.TRIPLE_QUOTED_STRING -> parseCodeLines(element)
            else -> null
        }
    }
}