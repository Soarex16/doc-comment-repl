package com.github.soarex16.doccommentrepl

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.task.ProjectTaskManager
import org.jetbrains.kotlin.KotlinIdeaReplBundle
import org.jetbrains.kotlin.console.KotlinConsoleKeeper
import org.jetbrains.kotlin.console.actions.errorNotification

class TestAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
                ?: return errorNotification(null, KotlinIdeaReplBundle.message("cannot.find.project"))
        val activeDocument = FileEditorManager.getInstance(project).selectedTextEditor?.document
                ?: return errorNotification(null, KotlinIdeaReplBundle.message("no.modules.were.found"))
        val activeDocFile = FileDocumentManager.getInstance().getFile(activeDocument)

        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(activeDocFile!!)!!

        // надо открыть какой-нибудь файл на котлине
        // потом наверху в панели tools нажать на Run DOC REPL и запуститься консолька

        ProjectTaskManager.getInstance(project).build(module).onSuccess {
            if (!module.isDisposed) {
                KotlinConsoleKeeper.getInstance(project).run(module, previousCompilationFailed = it.hasErrors())
            }
        }

        /*ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Starting DOC REPL") {
            override fun run(indicator: ProgressIndicator) {
                val cmdLine = KotlinConsoleKeeper.createReplCommandLine(project, module)
                val runner = KotlinConsoleRunner(
                        module,
                        cmdLine,
                        false,
                        project,
                        "DOC COMMENT REPL",
                        module.moduleFilePath
                )

                runner.initAndRun()
            }
        })*/
    }
}