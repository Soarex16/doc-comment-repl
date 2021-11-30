package com.github.soarex16.doccommentrepl.ui

import com.github.soarex16.doccommentrepl.DocCommentReplBundle
import com.github.soarex16.doccommentrepl.ExecuteSnippetAction
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.elementType

const val REPL_MARKER = ">>>"
const val MULTILINE_MARKER = "..." // TODO: добавить возможность выполнения многострочного кода

/**
 * PsiElement(KDOC_TEXT) - строчка в kdoc
 * PsiComment(EOL_COMMENT) - однострочный комментарий
 * PsiComment(BLOCK_COMMENT) - многострочный комментарий
 */
val COMMENT_NODE_TYPES = setOf("KDOC_TEXT", "EOL_COMMENT", "BLOCK_COMMENT")

private class ExecuteSnippetMarkerInfo(val snippet: String, callElement: PsiElement, val snippetTextRange: TextRange) : LineMarkerInfo<PsiElement>(
        callElement,
        snippetTextRange,
        AllIcons.RunConfigurations.TestState.Run,
        { DocCommentReplBundle.message("doccodecomment.tool.tip.text.execute") },
        null,
        GutterIconRenderer.Alignment.RIGHT,
        { DocCommentReplBundle.message("doccodecomment.tool.tip.text.execute") },
) {

    val callElementRef = SmartPointerManager.getInstance(callElement.project).createSmartPsiElementPointer(callElement)

    override fun createGutterRenderer(): GutterIconRenderer {
        return object : LineMarkerInfo.LineMarkerGutterIconRenderer<PsiElement>(this) {
            override fun getClickAction(): AnAction? {
                if (callElementRef.element?.isWritable != true) return null

                return ExecuteSnippetAction(snippet, callElementRef, snippetTextRange)
            }

            override fun isNavigateAction() = true

            override fun getTooltipText() = "Execute snippet ${(snippet)}"
        }
    }
}

class ExecuteSnippetLineMarkProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element is PsiWhiteSpace) return null
        if (element.elementType.toString() !in COMMENT_NODE_TYPES) return null

        val replMarkerOffset = element.text.indexOf(REPL_MARKER)
        if (replMarkerOffset == -1)
            return null

        val rangeLeftBorder = replMarkerOffset + REPL_MARKER.length
        val firstEol = element.text.indexOf('\n', rangeLeftBorder)
        val rangeRightBorder = if (firstEol == -1) element.text.length else firstEol

        val snippetRange = TextRange.from(
                element.textRange.startOffset + rangeLeftBorder,
                rangeRightBorder - rangeLeftBorder
        )

        val codeSnippet = element.text.substring(rangeLeftBorder, rangeRightBorder)

        return ExecuteSnippetMarkerInfo(codeSnippet, element, snippetRange)
    }
}