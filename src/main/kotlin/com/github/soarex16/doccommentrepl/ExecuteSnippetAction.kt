package com.github.soarex16.doccommentrepl

import com.github.soarex16.doccommentrepl.ui.COMMENT_NODE_TYPES
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.KotlinIdeaReplBundle
import org.jetbrains.kotlin.console.actions.errorNotification
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import javax.script.ScriptEngineManager

/**
 * >>> 1 + 2
 * res0: kotlin.Int = 3
 *
 * some text
 *
 * >>> "aBc".toUpperCase()
 * res1: kotlin.String = ABC
 */
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

        val executionResult = "abc"

        val commentString = executionResult.lines().joinToString { "\n//$it" }
        WriteCommandAction.runWriteCommandAction(project) {
            activeDocument.insertString(psiElement.textRange.endOffset, "$commentString\n")
        }
        /*val commentElement = createComment(psiElement, executionResult)

        psiElement.parent.addAfter(commentElement, psiElement)*/
    }
}