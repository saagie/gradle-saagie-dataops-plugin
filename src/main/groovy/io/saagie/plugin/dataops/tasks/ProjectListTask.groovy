package io.saagie.plugin.dataops.tasks

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class ProjectListTask extends DefaultTask {
    DataOpsExtension configuration
    SaagieClient saagieClient

    @TaskAction
    def projectList() {
        saagieClient = new SaagieClient(configuration)
        def response = saagieClient.getProjects()
        println response
        return response
    }
}
