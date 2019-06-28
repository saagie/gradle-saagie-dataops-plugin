package io.saagie.plugin.dataops.tasks

import io.saagie.plugin.dataops.DataOpsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class ProjectListTask extends DefaultTask {
    DataOpsExtension configuration

    @TaskAction
    def projectList() {
        println "Project list"
    }
}
