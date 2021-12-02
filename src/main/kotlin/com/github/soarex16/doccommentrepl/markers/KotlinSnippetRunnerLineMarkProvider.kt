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
}