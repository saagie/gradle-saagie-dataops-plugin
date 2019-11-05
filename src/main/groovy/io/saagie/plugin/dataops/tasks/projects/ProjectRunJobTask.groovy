package io.saagie.plugin.dataops.tasks.projects

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectRunJobTask extends DefaultTask {
    @Input DataOpsExtension configuration
    @Internal SaagieClient saagieClient

    @TaskAction
    def runProjectJob() {
        saagieClient = new SaagieClient(configuration)
        logger.quiet(saagieClient.runProjectJob())
    }
}
