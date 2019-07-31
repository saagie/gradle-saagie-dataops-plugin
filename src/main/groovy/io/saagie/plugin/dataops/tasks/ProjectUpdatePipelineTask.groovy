package io.saagie.plugin.dataops.tasks

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class ProjectUpdatePipelineTask extends DefaultTask {
    DataOpsExtension configuration
    SaagieClient saagieClient

    @TaskAction
    def createProjectPipeline() {
        saagieClient = new SaagieClient(configuration)
        logger.quiet(saagieClient.updateProjectPipeline())
    }
}
