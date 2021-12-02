package com.github.soarex16.doccommentrepl.python

import com.github.soarex16.doccommentrepl.DocCommentReplBundle
import com.github.soarex16.doccommentrepl.errorNotification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.actions.PyExecuteInConsole
import com.jetbrains.python.console.PyExecuteConsoleCustomizer

class ExecutePythonSnippetAction(val code: String, private val callElement: SmartPsiElementPointer<PsiElement>?, val snippetTextRange: TextRange) : AnAction() {
    constructor() : this("", null, TextRange.EMPTY_RANGE)

    override fun actionPerformed(event: AnActionEvent) {
        //val psiElement = callElement?.element ?: return

        val pyLangId = PythonLanguage.getInstance().id

        val project = event.project
                ?: return errorNotification(null, DocCommentReplBundle.message("cannot.find.project"))
        val activeDocument = FileEditorManager.getInstance(project).selectedTextEditor?.document
                ?: return errorNotification(null, DocCommentReplBundle.message("no.modules.were.found"))
        val activeDocFile = FileDocumentManager.getInstance().getFile(activeDocument)
        val editor = event.getData(CommonDataKeys.EDITOR)

        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(activeDocFile!!)!!

        val config = PyExecuteConsoleCustomizer.instance.getContextConfig(event.dataContext)
        PyExecuteInConsole
                .executeCodeInConsole(project, code, editor, true, true, false, config)

        // полезно для подмены файла
//        myPydevConsoleCommunication.setConsoleFile(consoleView.getVirtualFile())
//        consoleView.addMessageFilter(PythonTracebackFilter(myProject))

        /*runner.run(false)
        runner.addConsoleListener {
            runner.pydevConsoleCommunication.execInterpreter(ConsoleCommunication.ConsoleCodeFragment(testCode, true)) {
                val result = runner.consoleView.historyViewer.document.text

                errorNotification(null, result)

                return@execInterpreter null
            }
        }*/


        // CommandLineProcess createProcess

//        val client = PythonConsoleBackendService.AsyncClient.Factory

        /*runner.pydevConsoleCommunication.addCommunicationListener(object : ConsoleCommunicationListener {
            override fun commandExecuted(more: Boolean) {
                //val output = runner.consoleView.getHistoryViewer().getDocument().getText()
                WriteCommandAction.runWriteCommandAction(project) {
                    //val output = view.file.text
                    *//*val commentString = output.lines().joinToString { "\n#$it" }
                    activeDocument.insertString(psiElement.textRange.endOffset, commentString)*//*
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
        runner.consoleView.executeInConsole(code)*/
        //runner.pydevConsoleCommunication.
    }
}