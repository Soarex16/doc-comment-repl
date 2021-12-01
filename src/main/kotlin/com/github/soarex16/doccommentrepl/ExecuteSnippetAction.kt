package com.github.soarex16.doccommentrepl

import com.github.soarex16.doccommentrepl.repl.console.KotlinConsoleKeeper
import com.github.soarex16.doccommentrepl.repl.console.KotlinConsoleRunner
import com.github.soarex16.doccommentrepl.ui.COMMENT_NODE_TYPES
import com.intellij.execution.process.BaseOSProcessHandler
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.KotlinIdeaReplBundle
import org.jetbrains.kotlin.cli.common.repl.replInputAsXml
import org.jetbrains.kotlin.console.actions.errorNotification
import org.jetbrains.kotlin.console.actions.logError

fun errorNotification(project: Project?, message: String) {
    val errorTag = "DOCCOMMENT ERROR"
    val errorTitle = DocCommentReplBundle.message("doccodecomment.error.generic")
    Notifications.Bus.notify(Notification(errorTag, errorTitle, message, NotificationType.ERROR), project)
}

class ExecuteSnippetAction(val code: String, private val callElement: SmartPsiElementPointer<PsiElement>?, val snippetTextRange: TextRange) : AnAction() {
    constructor() : this("", null, TextRange.EMPTY_RANGE)

    private fun createComment(element: PsiElement, comment: String): PsiElement {
        val psiParser = PsiParserFacade.SERVICE.getInstance(element.project)

        if (element.elementType.toString() !in COMMENT_NODE_TYPES) return psiParser.createWhiteSpaceFromText("")

        return psiParser.createLineOrBlockCommentFromText(element.language, comment)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val psiElement = callElement?.element ?: return

        val project = event.project
                ?: return errorNotification(null, KotlinIdeaReplBundle.message("cannot.find.project"))
        val activeDocument = FileEditorManager.getInstance(project).selectedTextEditor?.document
                ?: return errorNotification(null, KotlinIdeaReplBundle.message("no.modules.were.found"))
        val activeDocFile = FileDocumentManager.getInstance().getFile(activeDocument)

        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(activeDocFile!!)!!

//        val executionResult = "abc"
//
//        val commentString = executionResult.lines().joinToString { "\n//$it" }
//        WriteCommandAction.runWriteCommandAction(project) {
//            activeDocument.insertString(psiElement.textRange.endOffset, "$commentString\n")
//        }

        // надо открыть какой-нибудь файл на котлине
        // потом наверху в панели tools нажать на Run DOC REPL и запуститься консолька

        val keeper = KotlinConsoleKeeper.getInstance(project)
        val runner: KotlinConsoleRunner
        if (keeper.currentRunner == null) {
            runner = keeper.run(module, previousCompilationFailed = false)
        } else {
            runner = keeper.currentRunner!!
        }
        runner.activeDocument = activeDocument
        runner.callElementRef = callElement
        sendCommandToProcess(code, runner)
    }

    private fun sendCommandToProcess(command: String, runner: KotlinConsoleRunner) {
        /*
        *
        Мне кажется на каждый вызов надо создавать свой virtual file (как в kotlin console runner)
        * LightVirtualFile(
                    "${consoleView.virtualFile.name}$lineNumber${KotlinParserDefinition.STD_SCRIPT_EXT}",
                    KotlinLanguage.INSTANCE, text
                )

        consoleView.virtualFile.rename(this, consoleView.virtualFile.name + KotlinParserDefinition.STD_SCRIPT_EXT)
        consoleView.virtualFile.putUserData(KOTLIN_CONSOLE_KEY, true)
        * */

        val processHandler = runner.processHandler
        val processInputOS =
            processHandler.processInput ?: return logError(this::class.java, "<p>Broken process stream</p>")
        val charset = (processHandler as? BaseOSProcessHandler)?.charset ?: Charsets.UTF_8

        val xmlRes = command.replInputAsXml()

        val bytes = ("$xmlRes\n").toByteArray(charset)
        processInputOS.write(bytes)
        processInputOS.flush()
    }
}