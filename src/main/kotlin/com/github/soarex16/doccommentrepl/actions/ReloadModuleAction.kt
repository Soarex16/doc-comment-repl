package com.github.soarex16.doccommentrepl.actions

import com.github.soarex16.doccommentrepl.repl.console.KotlinConsoleKeeper
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.task.ProjectTaskManager
import org.jetbrains.kotlin.KotlinIdeaReplBundle
import org.jetbrains.kotlin.console.actions.errorNotification

class ReloadModuleAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project =
            event.project ?: return errorNotification(null, KotlinIdeaReplBundle.message("cannot.find.project"))
        val keeper = KotlinConsoleKeeper.getInstance(project)
        ProjectTaskManager.getInstance(project).build(keeper.currentRunner!!.module).onSuccess {
            keeper.currentRunner = keeper.run(keeper.currentRunner!!.module)
        }
    }
}