package io.saagie.plugin.dataops.tasks.projects.graphPipeline

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.GraphPipelineClient
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectUpgradeGraphPipelineTask extends DefaultTask{
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
        saagieClient = new GraphPipelineClient(configuration, taskName)
        result = saagieClient.updateProjectGraphPipeline()
        logger.quiet(result)
        return result
    }
}
