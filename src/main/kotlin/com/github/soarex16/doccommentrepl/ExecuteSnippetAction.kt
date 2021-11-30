package com.github.soarex16.doccommentrepl

import com.github.soarex16.doccommentrepl.ui.COMMENT_NODE_TYPES
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.KotlinIdeaReplBundle
import org.jetbrains.kotlin.console.actions.errorNotification
import org.jetbrains.kotlin.idea.scratch.KtScratchFileLanguageProvider
import org.jetbrains.kotlin.idea.scratch.ScratchExpression
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutput
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutputHandler

class SnippetResultHandler(val project: Project, val doc: Document, val element: PsiElement): ScratchOutputHandler {
    override fun clear(file: ScratchFile) {

    }

    override fun error(file: ScratchFile, message: String) {

    }

    override fun handle(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {
        WriteCommandAction.runWriteCommandAction(project) {
            val commentString = output.text.lines().joinToString { "\n//$it" }
            doc.insertString(element.textRange.endOffset, commentString)
        }
    }

    override fun onFinish(file: ScratchFile) {

    }

    override fun onStart(file: ScratchFile) {

    }
}

class ExecuteSnippetAction(val code: String, private val callElement: SmartPsiElementPointer<PsiElement>?, val snippetTextRange: TextRange) : AnAction() {
    constructor() : this("", null, TextRange.EMPTY_RANGE)

    private fun createComment(element: PsiElement, comment: String): PsiElement {
        val psiParser = PsiParserFacade.SERVICE.getInstance(element.project)

        if (element.elementType.toString() !in COMMENT_NODE_TYPES) return psiParser.createWhiteSpaceFromText("")

        return psiParser.createLineOrBlockCommentFromText(element.language, comment)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val psiElement = callElement?.element ?: return

        val project = event.project
                ?: return errorNotification(null, KotlinIdeaReplBundle.message("cannot.find.project"))
        val activeDocument = FileEditorManager.getInstance(project).selectedTextEditor?.document
                ?: return errorNotification(null, KotlinIdeaReplBundle.message("no.modules.were.found"))
        val activeDocFile = FileDocumentManager.getInstance().getFile(activeDocument)

        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(activeDocFile!!)!!

        val vFile = PsiFileFactory
                .getInstance(project)
                .createFileFromText(psiElement.language, code)
                .virtualFile

        val scratch = KtScratchFileLanguageProvider().newScratchFile(project, vFile)

        val executor = scratch?.compilingScratchExecutor ?: return
        executor.addOutputHandler(SnippetResultHandler(project, activeDocument, psiElement))
        executor.execute()
    }
}