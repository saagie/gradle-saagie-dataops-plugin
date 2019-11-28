package io.saagie.plugin.dataops.tasks.projects

import groovy.transform.TypeChecked
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import io.saagie.plugin.dataops.models.Server
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

@TypeChecked
class ProjectsCreateTask extends DefaultTask {
    @Input DataOpsExtension configuration

    @Input String taskName

    @Internal SaagieClient saagieClient

    @TaskAction
    def createProject() {
        Server server = configuration.server
        saagieClient = new SaagieClient(configuration, taskName)

        def result = saagieClient.createProject()
        logger.quiet(result)
        return result
    }
}
