package com.github.soarex16.doccommentrepl.actions

import com.github.soarex16.doccommentrepl.repl.console.KotlinConsoleKeeper
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.task.ProjectTaskManager
import org.jetbrains.kotlin.KotlinIdeaReplBundle

class ReloadModuleAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
            ?: return com.github.soarex16.doccommentrepl.errorNotification(
                null,
                KotlinIdeaReplBundle.message("cannot.find.project")
            )
        val activeDocument = FileEditorManager.getInstance(project).selectedTextEditor?.document
            ?: return com.github.soarex16.doccommentrepl.errorNotification(
                null,
                KotlinIdeaReplBundle.message("no.modules.were.found")
            )
        val activeDocFile = FileDocumentManager.getInstance().getFile(activeDocument)

        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(activeDocFile!!)!!
        val keeper = KotlinConsoleKeeper.getInstance(project)
        ProjectTaskManager.getInstance(project).build(module).onSuccess {
            keeper.currentRunner = keeper.run(module)
        }
    }
}