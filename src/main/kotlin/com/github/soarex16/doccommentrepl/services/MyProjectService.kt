package com.github.soarex16.doccommentrepl.services

import com.intellij.openapi.project.Project
import com.github.soarex16.doccommentrepl.DocCommentReplBundle

class MyProjectService(project: Project) {

    init {
        println(DocCommentReplBundle.message("projectService", project.name))
    }
}
