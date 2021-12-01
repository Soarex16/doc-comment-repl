package com.github.soarex16.doccommentrepl.markers

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * Провайдер маркеров запуска сниппетов кода в Питоне
 */
class PythonSnippetRunnerLineMarkProvider : ExecuteSnippetRunnerLineMarkProvider() {
    override fun extractCodeSnippet(element: PsiElement): Pair<String, TextRange>? {
        TODO("Not yet implemented")
    }
}