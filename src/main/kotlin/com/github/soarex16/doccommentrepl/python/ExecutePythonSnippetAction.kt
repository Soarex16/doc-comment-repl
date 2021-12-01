package com.github.soarex16.doccommentrepl.python

import com.github.soarex16.doccommentrepl.DocCommentReplBundle
import com.github.soarex16.doccommentrepl.python.PydevConsoleRunner as DocCommentPydevConsoleRunner
import com.github.soarex16.doccommentrepl.ui.COMMENT_NODE_TYPES
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.elementType
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.console.*
import com.jetbrains.python.console.protocol.PythonConsoleBackendService
import com.jetbrains.python.console.pydev.ConsoleCommunication
import com.jetbrains.python.console.pydev.ConsoleCommunicationListener

fun errorNotification(project: Project?, message: String) {
    val errorTag = "DOCCOMMENT ERROR"
    val errorTitle = DocCommentReplBundle.message("doccodecomment.error.generic")
    Notifications.Bus.notify(Notification(errorTag, errorTitle, message, NotificationType.ERROR), project)
}

class ExecutePythonSnippetAction(val code: String, private val callElement: SmartPsiElementPointer<PsiElement>?, val snippetTextRange: TextRange) : AnAction() {
    constructor() : this("", null, TextRange.EMPTY_RANGE)

    private fun createComment(element: PsiElement, comment: String): PsiElement {
        val psiParser = PsiParserFacade.SERVICE.getInstance(element.project)

        if (element.elementType.toString() !in COMMENT_NODE_TYPES) return psiParser.createWhiteSpaceFromText("")

        return psiParser.createLineOrBlockCommentFromText(element.language, comment)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val psiElement = callElement?.element ?: return

        val langId = PythonLanguage.INSTANCE.id

        val project = event.project
                ?: return errorNotification(null, DocCommentReplBundle.message("cannot.find.project"))
        val activeDocument = FileEditorManager.getInstance(project).selectedTextEditor?.document
                ?: return errorNotification(null, DocCommentReplBundle.message("no.modules.were.found"))
        val activeDocFile = FileDocumentManager.getInstance().getFile(activeDocument)

        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(activeDocFile!!)!!

        val runner: PydevConsoleRunner = PythonConsoleRunnerFactory.getInstance().createConsoleRunner(project, null)

        // CommandLineProcess createProcess

//        val client = PythonConsoleBackendService.AsyncClient.Factory

        runner.pydevConsoleCommunication.addCommunicationListener(object : ConsoleCommunicationListener {
            override fun commandExecuted(more: Boolean) {
                //val output = runner.consoleView.getHistoryViewer().getDocument().getText()
                WriteCommandAction.runWriteCommandAction(project) {
                    //val output = view.file.text
                    /*val commentString = output.lines().joinToString { "\n#$it" }
                    activeDocument.insertString(psiElement.textRange.endOffset, commentString)*/
                }
            }

            override fun inputRequested() {
                TODO("Not yet implemented")
            }
        })

        runner.addConsoleListener{ view ->
            WriteCommandAction.runWriteCommandAction(project) {
                val output = view.file.text
                val commentString = output.lines().joinToString { "\n//$it" }
                activeDocument.insertString(psiElement.textRange.endOffset, commentString)
            }
        }

        runner.run(true)
        runner.consoleView.executeInConsole(code)
        //runner.pydevConsoleCommunication.
    }
}