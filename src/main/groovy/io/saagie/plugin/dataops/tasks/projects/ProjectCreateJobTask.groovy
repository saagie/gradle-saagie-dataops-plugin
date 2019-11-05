package io.saagie.plugin.dataops.tasks.projects

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectCreateJobTask extends DefaultTask {
    @Input DataOpsExtension configuration
    @Internal SaagieClient saagieClient

    @TaskAction
    def createProjectJob() {
        saagieClient = new SaagieClient(configuration)
        logger.quiet(saagieClient.createProjectJobWithGraphQL())
    }
}