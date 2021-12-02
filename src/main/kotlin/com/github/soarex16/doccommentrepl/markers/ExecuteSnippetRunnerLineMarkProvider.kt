package com.github.soarex16.doccommentrepl.markers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Абстрактный класс для создания маркеров запуска сниппетов для любого языка
 */
abstract class ExecuteSnippetRunnerLineMarkProvider: LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (!isApplicable(element)) return null

        val (codeSnippet, snippetRange) = extractCodeSnippet(element) ?: return null

        return ExecuteSnippetMarkerInfo(codeSnippet, element, snippetRange)
    }

    open fun isApplicable(element: PsiElement): Boolean {
        return element is PsiComment && element.text.indexOf(SNIPPET_START_MARKER) != -1
    }

    abstract fun extractCodeSnippet(element: PsiElement): Pair<String, TextRange>?
}

/**
 * Parse multiline comment as set of lines
 */
fun parseCodeLines(element: PsiElement): Pair<String, TextRange> {
    val commentText = element.text
    val replMarkerRelativeOffset = element.text.indexOf(SNIPPET_START_MARKER)

    val commentLines = commentText
            .substring(replMarkerRelativeOffset)
            .replaceFirst(SNIPPET_START_MARKER, MULTILINE_MARKER) // заменяем для единообразия дальнейшей обработки
            .lines()

    var endPosition = replMarkerRelativeOffset
    val sb = StringBuilder()
    for (line in commentLines) {
        val markerIndex = line.indexOf(MULTILINE_MARKER)

        if (markerIndex == -1)
            break

        endPosition += line.length
        sb.appendLine(line.substring(markerIndex + MULTILINE_MARKER.length))
    }

    val snippetRange = TextRange.from(
            element.startOffset + replMarkerRelativeOffset,
            endPosition - replMarkerRelativeOffset
    )

    // TODO: не учитывается случай, когда последняя строка содержит окончание комментария - */
    return Pair(sb.toString(), snippetRange)
}

/**
 * Parse single-line comment and try parse right siblings
 */
fun tryParseNextEolComments(element: PsiElement): Pair<String, TextRange> {
    val sb = StringBuilder()

    val replMarkerRelativeOffset = element.text.indexOf(SNIPPET_START_MARKER)
    sb.appendLine(element.text.substring(replMarkerRelativeOffset + SNIPPET_START_MARKER.length))

    var endPosition = element.text.length
    var commentElement = element
    while (true) {
        commentElement = commentElement.nextSibling ?: break
        var spaceSymbols = 0
        while (commentElement is PsiWhiteSpace) {
            spaceSymbols += commentElement.text.length
            commentElement = commentElement.nextSibling ?: break
        }

        if (commentElement !is PsiComment)
            break

        endPosition += spaceSymbols

        val commentText = commentElement.text
        val markerIndex = commentText.indexOf(MULTILINE_MARKER)

        if (markerIndex == -1)
            break

        endPosition += commentText.length
        sb.appendLine(commentText.substring(markerIndex + MULTILINE_MARKER.length))
    }

    val snippetRange = TextRange.from(
            element.startOffset + replMarkerRelativeOffset,
            endPosition - replMarkerRelativeOffset
    )

    return Pair(sb.toString(), snippetRange)
}