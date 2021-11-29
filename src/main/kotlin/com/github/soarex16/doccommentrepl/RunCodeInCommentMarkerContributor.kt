package com.github.soarex16.doccommentrepl

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.util.Function
import java.util.function.Supplier

const val REPL_MARKER = ">>>"

class RunCodeInCommentMarkerContributor : RunLineMarkerContributor() {
    val tooltip = Function<PsiElement, String>{ "Run in REPL" }

    /**
    PsiComment(EOL_COMMENT) - однострочный комментарий
    PsiElement(KDOC_TEXT) - строчка в kdoc
    PsiComment(BLOCK_COMMENT) - многострочный комментарий
     */
    override fun getInfo(element: PsiElement): Info? {
        //if (element !is PsiComment) return null

        if (!element.text.contains(REPL_MARKER))
            return null

        return when (element.elementType.toString()) {
            "EOL_COMMENT", "KDOC_TEXT" -> Info(AllIcons.Actions.RunAll, tooltip, TestAction())
            else -> null
        }

        //val tooltip: Function<PsiElement, String> =
    }
}

class SampleLineMarkProvider : LineMarkerProvider {
    private val nameSupplier = Supplier { "Sample" }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {


        val replMarkerOffset = element.text.indexOf(REPL_MARKER)

        if (replMarkerOffset == -1)
            return null

        val firstEol = element.text.indexOf('\n', replMarkerOffset + REPL_MARKER.length)
        val endPosition = if (firstEol == -1) element.text.length else firstEol

        val markerRange = TextRange.from(
                element.textRange.startOffset + replMarkerOffset,
                 element.textRange.startOffset + endPosition
        )

        val markerInfo = LineMarkerInfo(element, markerRange, AllIcons.Actions.Run_anything, null, null, GutterIconRenderer.Alignment.LEFT, nameSupplier)
        return when (element.elementType.toString()) {
            "KDOC_TEXT" -> markerInfo
            "EOL_COMMENT" -> markerInfo
            else -> null
        }
    }
}