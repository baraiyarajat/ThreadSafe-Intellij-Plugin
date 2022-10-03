package com.contemplateltd.intellij.threadsafeintegration.services

import com.intellij.openapi.project.Project
import com.contemplateltd.intellij.threadsafeintegration.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
