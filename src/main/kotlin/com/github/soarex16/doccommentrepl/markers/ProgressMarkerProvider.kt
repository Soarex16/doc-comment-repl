package com.github.soarex16.doccommentrepl.markers

import com.github.soarex16.doccommentrepl.DocCommentReplBundle
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.ui.AnimatedIcon
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ProgressMarkerInfo(callElement: PsiElement, snippetTextRange: TextRange) : LineMarkerInfo<PsiElement>(
        callElement,
        snippetTextRange,
        AnimatedIcon.Default.INSTANCE,
        { DocCommentReplBundle.message("doccodecomment.tool.tip.text.execute") },
        null,
        GutterIconRenderer.Alignment.RIGHT,
        { DocCommentReplBundle.message("doccodecomment.tool.tip.text.execute") },
) {
    override fun createGutterRenderer(): GutterIconRenderer {
        return object : LineMarkerGutterIconRenderer<PsiElement>(this) {
            override fun getClickAction(): AnAction? = null

            override fun isNavigateAction() = false

            override fun getTooltipText() = DocCommentReplBundle.message("doccodecomment.tool.tip.text.running")
        }
    }
}

class ProgressMarkerProvider: LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // TODO: python  triple quoted strings
        if (element !is PsiComment) return null

        val executingMarkerRelativePosition = element.text.indexOf(EXECUTING_MARKER)

        if (executingMarkerRelativePosition == -1) return null

        return ProgressMarkerInfo(element, TextRange.from(element.startOffset, EXECUTING_MARKER.length))
    }
}