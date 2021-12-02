package com.github.soarex16.doccommentrepl.actions

import com.github.soarex16.doccommentrepl.DocCommentReplBundle
import com.github.soarex16.doccommentrepl.errorNotification
import com.github.soarex16.doccommentrepl.execution.*
import com.github.soarex16.doccommentrepl.markers.EXECUTING_MARKER
import com.github.soarex16.doccommentrepl.markers.REPL_OUTPUT_MARKER
import com.github.soarex16.doccommentrepl.markers.SNIPPET_START_MARKER
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.KotlinIdeaReplBundle
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.startOffset

val cowsay = """
  ________________________________________
/ Да, оно не работает. Да, оно сломалось.  \
| И что ты мне сделаешь?                   |
\ Я ведь всего-лишь корова...              /
  ----------------------------------------
         \   ^__^ 
          \  (oo)\_______
             (__)\       )\/\
                 ||----w |
                 ||     ||
""".trimIndent()

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

        val executor = when (psiElement.language.id) {
            "kotlin" -> EXECUTORS[SnippetLanguage.Kotlin]
            "Python" -> EXECUTORS[SnippetLanguage.Python]
            else -> return errorNotification(project, DocCommentReplBundle.message("executor.not.found"))
        }!!

        val (snippetCode, position) = executor.parseSnippet(psiElement) ?: return errorNotification(
            project,
            DocCommentReplBundle.message("parse.error")
        )

        val markerPos = snippetCode.indexOf(SNIPPET_START_MARKER) + position.startOffset + 1

        try {
            WriteCommandAction.runWriteCommandAction(project) {
                activeDocument.replaceString(markerPos, markerPos + SNIPPET_START_MARKER.length, EXECUTING_MARKER)
            }

            val ctx = ExecutionContext(event, snippetCode, project, module) { res ->
                WriteCommandAction.runWriteCommandAction(project) {
                    val actualCallElement = callElement.element

                    if (actualCallElement == null) {
                        org.jetbrains.kotlin.console.actions.errorNotification(
                            project,
                            DocCommentReplBundle.message("doccodecomment.error.document.modified")
                        )
                        return@runWriteCommandAction
                    }

                    val commentString = executor.formatComment(res.trim())

                    val nextElement = actualCallElement.getNextSiblingIgnoringWhitespace()
                    if (nextElement is PsiElement && nextElement.text.contains(REPL_OUTPUT_MARKER)) {
                        activeDocument.replaceString(nextElement.startOffset, nextElement.endOffset, commentString)
                    } else {
                        activeDocument.insertString(actualCallElement.textRange.endOffset, "\n" + commentString)
                    }
                }
            }

            executor.executeSnippet(ctx)
        } catch (e: Exception) {
            errorNotification(project, e.localizedMessage)

            val cowsayComment = executor.formatComment(cowsay)

            WriteCommandAction.runWriteCommandAction(project) {
                val actualCallElement = callElement.element ?: return@runWriteCommandAction
                val nextElement = actualCallElement.getNextSiblingIgnoringWhitespace()
                if (nextElement is PsiElement && nextElement.text.contains(REPL_OUTPUT_MARKER)) {
                    activeDocument.replaceString(nextElement.startOffset, nextElement.endOffset, cowsayComment)
                } else {
                    activeDocument.insertString(actualCallElement.textRange.endOffset, "\n" + cowsayComment)
                }
            }
        } finally {
            WriteCommandAction.runWriteCommandAction(project) {
                activeDocument.replaceString(markerPos, markerPos + SNIPPET_START_MARKER.length, SNIPPET_START_MARKER)
            }
        }
    }
}