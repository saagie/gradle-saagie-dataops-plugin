package io.saagie.plugin.dataops.clients

import groovy.json.JsonOutput
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Pipeline
import io.saagie.plugin.dataops.models.PipelineVersion
import okhttp3.Request
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class GraphPipelineClient extends SaagieClient{
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

    String createProjectGraphPipeline(Pipeline pipeline, PipelineVersion graphPipelineVersion) {
        logger.info('Starting createGraphPipeline task')
        checkRequiredConfig(!configuration?.project?.id || !pipeline?.name)

        logger.debug('Using config [project={}, pipeline={}, graphPipelineVersion={}]', configuration.project, pipeline, graphPipelineVersion)

        Request createPipelineRequest = saagieUtils.getCreateGraphPipelineRequest(pipeline, graphPipelineVersion)
        tryCatchClosure({
            client.newCall(createPipelineRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when creating the graph pipeline: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map createdPipeline = parsedResult.data.createGraphPipeline
                    return JsonOutput.toJson(createdPipeline)
                }
            }
        }, 'Unknown error in createGraphPipeline')
    }

    String updateProjectGraphPipeline() {
        logger.info('Starting upgradeProjectGraphPipeline task')

        // 1. try to update pipeline infos
        checkRequiredConfig(!configuration?.pipeline?.id)
        def pipelineResult = updatePipelineInfos()

        // 2. try to update pipeline version
        if (configuration?.pipelineVersion) {
            checkRequiredConfig(configuration?.pipelineVersion && configuration?.pipelineVersion?.graph?.jobNodes?.isEmpty())
            updateGraphPipelineVersion(configuration?.pipeline, configuration?.pipelineVersion)
        }

        return pipelineResult
    }

    private updateGraphPipelineVersion(Pipeline pipeline, PipelineVersion graphPipelineVersion) {
        logger.debug('Calling updateGraphPipelineVersion')
        logger.debug('Using config [pipeline={}]', pipeline)
        logger.debug('Using config [graphPipelineVersion={}]', graphPipelineVersion)
        Request updatePipelineVersionRequest = saagieUtils.getAddGraphPipelineVersionRequest(pipeline, graphPipelineVersion)
        tryCatchClosure({
            client.newCall(updatePipelineVersionRequest).execute().withCloseable { updateResponse ->
                handleErrors(updateResponse)
                String updateResponseBody = updateResponse.body().string()
                def updatedPipelineVersion = slurper.parseText(updateResponseBody)
                if (updatedPipelineVersion.data == null) {
                    def message = "Something went wrong when adding new project graph pipeline version: $updateResponseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    String newPipelineVersion = updatedPipelineVersion.data.addGraphPipelineVersion.number
                    logger.info('Updated pipelineVersion number: {}', newPipelineVersion)
                }
            }
        }, 'Unknown error in updateGraphPipelineVersion')
    }
}
