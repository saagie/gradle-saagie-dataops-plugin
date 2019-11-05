package io.saagie.plugin.dataops.tasks.platform

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PlatformListTask extends DefaultTask {
    DataOpsExtension configuration
    SaagieClient saagieClient

    @TaskAction
    def archiveProjectJob() {
        saagieClient = new SaagieClient(configuration)
        logger.quiet(saagieClient.listPlatforms())
    }
}
