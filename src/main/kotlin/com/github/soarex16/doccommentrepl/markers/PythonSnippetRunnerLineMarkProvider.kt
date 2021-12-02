package com.github.soarex16.doccommentrepl.markers

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.jetbrains.python.PyTokenTypes

/**
 * Провайдер маркеров запуска сниппетов кода в Питоне
 */
class PythonSnippetRunnerLineMarkProvider : ExecuteSnippetRunnerLineMarkProvider() {
    override fun extractCodeSnippet(element: PsiElement): Pair<String, TextRange>? {
        val elType = element.elementType
        // PyTokenTypes.END_OF_LINE_COMMENT
        // PyTokenTypes.DOCSTRING

        val isComment = element is PsiComment

        return null
    }
}