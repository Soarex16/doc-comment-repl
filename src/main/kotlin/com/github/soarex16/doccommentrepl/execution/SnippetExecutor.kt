package com.github.soarex16.doccommentrepl.execution

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

data class ExecutionContext(
    val event: AnActionEvent,
    val code: String,
    val project: Project,
    val module: Module,
    val onSnippetExecuted: (execResult: String) -> Unit
)

enum class SnippetLanguage(langId: String) {
    Kotlin("kotlin"),
    Python("Python")
}

interface SnippetExecutor {
    fun formatComment(execResult: String): String

    fun parseSnippet(element: PsiElement): Pair<String, TextRange>?

    fun executeSnippet(ctx: ExecutionContext): Any
}