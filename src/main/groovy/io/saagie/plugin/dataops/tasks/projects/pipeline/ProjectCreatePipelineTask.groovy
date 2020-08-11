package io.saagie.plugin.dataops.tasks.projects.pipeline

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectCreatePipelineTask extends DefaultTask {
    @Input DataOpsExtension configuration

    @Input String taskName

    @Internal SaagieClient saagieClient

    String result

    @TaskAction
    def createProjectPipeline() {
        saagieClient = new SaagieClient(configuration, taskName)
        def resultData = saagieClient.createProjectPipeline(configuration.pipeline, configuration.pipelineVersion)
        logger.quiet(resultData)
        result = resultData
        return result
    }
}
