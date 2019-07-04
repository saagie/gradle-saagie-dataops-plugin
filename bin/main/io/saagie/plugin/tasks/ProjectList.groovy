package io.saagie.plugin.dataops.tasks

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class ProjectList extends DefaultTask {

    @TaskAction
    def projectList() {
        println "Project list"
    }
}
