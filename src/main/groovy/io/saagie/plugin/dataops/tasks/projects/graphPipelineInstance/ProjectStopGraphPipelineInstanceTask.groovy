package io.saagie.plugin.dataops.tasks.projects.graphPipelineInstance

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.GraphPipelineClient
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectStopGraphPipelineInstanceTask extends DefaultTask {
    @Input
    DataOpsExtension configuration

    @Input
    String taskName

    @Internal
    SaagieClient saagieClient

    @Internal
    String result

    @TaskAction
    def stopProjectGraphPipelineInstance() {
        saagieClient = new GraphPipelineClient(configuration, taskName)
        result = saagieClient.stopGraphPipelineInstance()
        logger.quiet(result)
        return result
    }
}
