package io.saagie.plugin.dataops.tasks

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectUpdateJob extends DefaultTask {
    @Input DataOpsExtension configuration
    @Internal SaagieClient saagieClient

    @TaskAction
    def getProjectJobs() {
        saagieClient = new SaagieClient(configuration)
        logger.quiet(saagieClient.updateProjectJob())
    }
}
