package io.saagie.plugin.dataops.tasks

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class ProjectArchiveJobTask extends DefaultTask {
    DataOpsExtension configuration
    SaagieClient saagieClient

    @TaskAction
    def archiveProjectJob() {
        saagieClient = new SaagieClient(configuration)
        logger.quiet(saagieClient.archiveProjectJob())
    }
}