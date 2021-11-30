package com.github.soarex16.doccommentrepl.ui

import com.intellij.lang.injection.general.Injection
import com.intellij.lang.injection.general.LanguageInjectionContributor
import com.intellij.lang.injection.general.SimpleInjection
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

class KotlinCommentLanguageInjection: LanguageInjectionContributor {
    override fun getInjection(context: PsiElement): Injection? {
        if (context !is PsiComment) return null

        if (!context.text.contains(REPL_MARKER)) return null

        return SimpleInjection(context.language.id, "", "", null)
    }
}