package io.saagie.plugin.dataops.clients

import groovy.json.JsonOutput
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Pipeline
import io.saagie.plugin.dataops.models.graphPipeline.GraphPipelineVersion
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

        Request platformListRequest = saagieUtils.getListAllProjectGraphPipelinesRequest()
        tryCatchClosure({
            client.newCall(platformListRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when getting graph pipelines list: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    List pipelineList = parsedResult.data.project.pipelines
                    return JsonOutput.toJson(pipelineList)
                }
            }
        }, 'Unknown error in Task: projectsListAllGraphPipelines', 'Function: listAllGraphPipelines')
    }

    String createProjectGraphPipeline(Pipeline pipeline, GraphPipelineVersion graphPipelineVersion) {
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
        if (configuration?.graphPipelineVersion) {
            checkRequiredConfig(configuration?.graphPipelineVersion && configuration?.graphPipelineVersion?.graph?.jobNodes?.isEmpty())
            updateGraphPipelineVersion(configuration?.pipeline, configuration?.graphPipelineVersion)
        }

        return pipelineResult
    }

    private updateGraphPipelineVersion(Pipeline pipeline, GraphPipelineVersion graphPipelineVersion) {
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
