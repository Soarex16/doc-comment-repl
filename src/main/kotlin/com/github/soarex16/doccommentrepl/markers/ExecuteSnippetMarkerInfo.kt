package com.github.soarex16.doccommentrepl.markers

import com.github.soarex16.doccommentrepl.DocCommentReplBundle
import com.github.soarex16.doccommentrepl.actions.ExecuteSnippetAction
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager

/**
 * MarkerInformation for gutter "Execute code snippet" button
 */
class ExecuteSnippetMarkerInfo(callElement: PsiElement, snippetTextRange: TextRange) : LineMarkerInfo<PsiElement>(
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
        return object : LineMarkerGutterIconRenderer<PsiElement>(this) {
            override fun getClickAction(): AnAction? {
                if (callElementRef.element?.isWritable != true) return null

                return ExecuteSnippetAction(callElementRef)
            }

            override fun isNavigateAction() = true

            override fun getTooltipText() = DocCommentReplBundle.message("doccodecomment.tool.tip.text.execute")
        }
    }
}