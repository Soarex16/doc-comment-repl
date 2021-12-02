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

fun Any.logError(message: String, t: Throwable? = null) = with(Logger.getInstance(this.javaClass)) { warn(message, t) }

fun Any.logWarn(message: String, t: Throwable? = null) = with(Logger.getInstance(this.javaClass)) { warn(message, t) }