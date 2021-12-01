package com.github.soarex16.doccommentrepl.markers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

/**
 * Абстрактный класс для создания маркеров запуска сниппетов для любого языка
 */
abstract class ExecuteSnippetRunnerLineMarkProvider: LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (!isApplicable(element)) return null

        val (codeSnippet, snippetRange) = extractCodeSnippet(element) ?: return null

        return ExecuteSnippetMarkerInfo(codeSnippet, element, snippetRange)
    }

    fun isApplicable(element: PsiElement): Boolean {
        return element is PsiComment && element.text.indexOf(SNIPPET_START_MARKER) != -1
    }

    abstract fun extractCodeSnippet(element: PsiElement): Pair<String, TextRange>?
}