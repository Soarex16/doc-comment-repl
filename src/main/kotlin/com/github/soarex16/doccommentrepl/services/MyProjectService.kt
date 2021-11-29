package com.github.soarex16.doccommentrepl.services

import com.intellij.openapi.project.Project
import com.github.soarex16.doccommentrepl.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
