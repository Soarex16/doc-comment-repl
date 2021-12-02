package com.github.soarex16.doccommentrepl.actions

import com.github.soarex16.doccommentrepl.DocCommentReplBundle
import com.github.soarex16.doccommentrepl.errorNotification
import com.github.soarex16.doccommentrepl.execution.KotlinExecutor
import com.github.soarex16.doccommentrepl.execution.PythonExecutor
import com.github.soarex16.doccommentrepl.execution.SnippetExecutor
import com.github.soarex16.doccommentrepl.execution.SnippetLanguage
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.KotlinIdeaReplBundle

class ExecuteSnippetAction(private val callElement: SmartPsiElementPointer<PsiElement>?) : AnAction() {
    companion object {
        val EXECUTORS = mapOf<SnippetLanguage, SnippetExecutor>(
            SnippetLanguage.Kotlin to KotlinExecutor(),
            SnippetLanguage.Python to PythonExecutor()
        )
    }

    override fun actionPerformed(event: AnActionEvent) {
        val psiElement = callElement?.element ?: return

        val project = event.project
            ?: return errorNotification(null, KotlinIdeaReplBundle.message("cannot.find.project"))
        val activeDocument = FileEditorManager.getInstance(project).selectedTextEditor?.document
            ?: return errorNotification(null, KotlinIdeaReplBundle.message("no.modules.were.found"))
        val activeDocFile = FileDocumentManager.getInstance().getFile(activeDocument)

        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(activeDocFile!!)!!

        val executor = when(psiElement.language.id) {
            "kotlin" -> EXECUTORS[SnippetLanguage.Kotlin]
            "Python" -> EXECUTORS[SnippetLanguage.Python]
            else -> return errorNotification(project, DocCommentReplBundle.message("executor.not.found"))
        }!!

        val (snippetCode, position) = executor.parseSnippet(psiElement) ?: return errorNotification(project, DocCommentReplBundle.message("parse.error"))
        val executionResult = executor.executeSnippet(event, callElement, snippetCode, project, module, activeDocument) ?: return

        val comment = executor.formatComment(executionResult)
    }
}