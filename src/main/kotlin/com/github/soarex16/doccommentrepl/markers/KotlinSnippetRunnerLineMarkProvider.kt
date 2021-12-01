package com.github.soarex16.doccommentrepl.markers

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Провайдер маркеров запуска сниппетов кода в Котлине
 */
class KotlinSnippetRunnerLineMarkProvider : ExecuteSnippetRunnerLineMarkProvider() {
    override fun extractCodeSnippet(element: PsiElement): Pair<String, TextRange>? {
        /*
        NOTE: не учтен случай, когда в многострочном комментарии (BLOCK_COMMENT, DOC_COMMENT)
        несколько сниппетов. Например:

        >>> 1 + 2
        ...some text
        >>> 2 + 3

         */
        return when (element.elementType) {
            KtTokens.DOC_COMMENT, KtTokens.BLOCK_COMMENT -> parseCodeLines(element)
            KtTokens.EOL_COMMENT -> tryParseNextEolComments(element)
            else -> null
        }
    }

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

    fun tryParseNextEolComments(element: PsiElement): Pair<String, TextRange> {
        val sb = StringBuilder()

        val replMarkerRelativeOffset = element.text.indexOf(SNIPPET_START_MARKER)
        sb.appendLine(element.text.substring(replMarkerRelativeOffset + SNIPPET_START_MARKER.length))

        var endPosition = element.text.length
        var commentElement = element
        while (true) {
            commentElement = commentElement.nextSibling
            var spaceSymbols = 0
            while (commentElement is PsiWhiteSpace) {
                spaceSymbols += commentElement.text.length
                commentElement = commentElement.nextSibling
            }

            if (commentElement.elementType != KtTokens.EOL_COMMENT)
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
}