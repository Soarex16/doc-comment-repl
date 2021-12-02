package com.github.soarex16.doccommentrepl.execution

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer

enum class SnippetLanguage(langId: String) {
    Kotlin("kotlin"),
    Python("Python")
}

interface SnippetExecutor {
    fun formatComment(execResult: String): String

    fun parseSnippet(element: PsiElement): Pair<String, TextRange>?

    fun executeSnippet(event: AnActionEvent, callElement: SmartPsiElementPointer<PsiElement>, code: String, project: Project, module: Module, activeDocument: Document): String?
}