package io.saagie.plugin.dataops.tasks.projects.pipeline

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

// Deprecated since graph pipelines
@Deprecated
class ProjectUpgradePipelineTask extends DefaultTask {
    @Input
    DataOpsExtension configuration

    @Input
    String taskName

    @Internal
    SaagieClient saagieClient

    @Internal
    String result

    @TaskAction
    def updateProjectPipeline() {
        saagieClient = new SaagieClient(configuration, taskName)
        result = saagieClient.updateProjectPipeline()
        logger.quiet(result)
        return result
    }
}
