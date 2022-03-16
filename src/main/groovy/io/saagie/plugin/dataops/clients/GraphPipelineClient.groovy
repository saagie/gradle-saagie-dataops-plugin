package io.saagie.plugin.dataops.clients

import groovy.json.JsonOutput
import io.saagie.plugin.dataops.DataOpsExtension
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class GraphPipelineClient extends SaagieClient {
    static final Logger logger = Logging.getLogger(GraphPipelineClient.class)

    GraphPipelineClient(DataOpsExtension configuration, String taskName) {
        super(configuration, taskName)
    }

    String listAllGraphPipelines() {
        logger.info('Starting projectsListAllGraphPipelines task')
        checkRequiredConfig(!configuration?.project?.id)

        def pipelineList = getAllGraphPipelines()
        return JsonOutput.toJson(pipelineList)
    }

    String updateProjectGraphPipeline() {
        logger.info('Starting upgradeProjectGraphPipeline task')

        // 1. try to update pipeline infos
        checkRequiredConfig(!configuration?.pipeline?.id)
        def pipelineResult = updatePipelineInfos()

        // 2. try to update pipeline version
        if (configuration?.pipelineVersion) {
            checkRequiredConfig(configuration?.pipelineVersion && configuration?.pipelineVersion?.graph?.jobNodes?.isEmpty())
            updateGraphPipelineVersion(configuration?.pipeline, configuration?.pipelineVersion, false)
        }

        return pipelineResult
    }
}
