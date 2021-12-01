package com.github.soarex16.doccommentrepl

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

fun errorNotification(project: Project?, message: String) {
    val errorTag = "COMMENT_REPL ERROR"
    val errorTitle = DocCommentReplBundle.message("doccodecomment.error.generic")
    Notifications.Bus.notify(Notification(errorTag, errorTitle, message, NotificationType.ERROR), project)
}

fun logError(cl: Class<*>, message: String, t: Throwable? = null) = with(Logger.getInstance(cl)) { error(message, t) }