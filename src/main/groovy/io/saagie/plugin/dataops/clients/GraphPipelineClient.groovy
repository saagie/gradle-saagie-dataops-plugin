package io.saagie.plugin.dataops.clients

import groovy.json.JsonOutput
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Pipeline
import io.saagie.plugin.dataops.models.PipelineVersion
import okhttp3.Request
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

// TODO 2875
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

    String createProjectGraphPipeline(Pipeline pipeline, PipelineVersion pipelineVersion) {
        logger.info('Starting createPipeline task')
        checkRequiredConfig(!configuration?.project?.id || !pipeline?.name)

        logger.debug('Using config [project={}, pipeline={}, pipelineVersion={}]', configuration.project, pipeline, pipelineVersion)

        Request createPipelineRequest = saagieUtils.getCreatePipelineRequest(pipeline, pipelineVersion)
        tryCatchClosure({
            client.newCall(createPipelineRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when creating the pipeline: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map createdPipeline = parsedResult.data.createPipeline
                    return JsonOutput.toJson(createdPipeline)
                }
            }
        }, 'Unknown error in createPipeline')
    }

    //TODO voir si on garde, même fonction que pour les pipelines lineaire
    String deleteProjectGraphPipeline() {
        logger.info('Starting deleteProjectGraphPipeline task')
        checkRequiredConfig(!configuration?.pipeline?.id)

        logger.debug('Using config [pipelineId={}]', configuration.pipeline.id)

        Request projectDeletePipelineRequest = saagieUtils.getProjectDeletePipelineRequest()
        tryCatchClosure({
            client.newCall(projectDeletePipelineRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when deleting graph pipeline: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    String deletedPipelineStatus = parsedResult.data.deletePipeline ? 'success' : 'failure'
                    Map deletedPipeline = [status: deletedPipelineStatus]
                    return JsonOutput.toJson(deletedPipeline)
                }
            }
        }, 'Unknown error in Task: deleteProjectGraphPipeline', 'Function : deleteProjectGraphPipeline')
    }

    //TODO voir si on garde, même fonction que pour les pipelines lineaire
    String runProjectGraphPipeline() {
        logger.info('Starting runProjectGraphPipeline task')
        checkRequiredConfig(!configuration?.pipeline?.id)

        logger.debug('Using config [pipelineId={}]', configuration.pipeline.id)

        Request projectRunPipelineRequest = saagieUtils.getProjectRunPipelineRequest()
        Map runPipelineData
        tryCatchClosure({
            client.newCall(projectRunPipelineRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when running graph pipeline: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    runPipelineData = parsedResult.data
                }
            }

            if (runPipelineData?.runPipeline?.id) {
                Request projectGetPipelineInstanceStatus = saagieUtils.getProjectPipelineInstanceStatusRequestWithparam(runPipelineData.runPipeline.id)
                client.newCall(projectGetPipelineInstanceStatus).execute().withCloseable { response ->
                    handleErrors(response)
                    String responseBody = response.body().string()
                    def parsedResult = slurper.parseText(responseBody)
                    if (parsedResult.data == null) {
                        def message = "Something went wrong when intance graph pipeline: $responseBody"
                        logger.error(message)
                        throw new GradleException(message)
                    } else {
                        Map instancePipelineData = parsedResult.data
                        Map updatedPipeline = [runPipeline: [id: runPipelineData.runPipeline.id, status: instancePipelineData.pipelineInstance.status]]
                        return JsonOutput.toJson(updatedPipeline)
                    }
                }
            } else {
                def message = "Something went wrong when intance graph pipeline"
                logger.error(message)
                throw new GradleException(message)
            }
        }, 'Unknown error in Task: runProjectGraphPipeline', 'Function: runProjectGraphPipeline')
    }

    String updateProjectGraphPipeline() {
        logger.info('Starting upgradeProjectPipeline task')

        // 1. try to update pipeline infos
        checkRequiredConfig(!configuration?.pipeline?.id)
        def pipelineResult = updatePipelineInfos()

        // 2. try to update pipeline version
        if (configuration?.pipelineVersion) {
            checkRequiredConfig(configuration?.pipelineVersion && configuration?.pipelineVersion?.jobs?.isEmpty())
            updatePipelineVersion(configuration?.pipeline, configuration?.pipelineVersion)
        }

        return pipelineResult
    }

    //TODO voir si on garde, même fonction que pour les pipelines lineaire
    String stopGraphPipelineInstance() {
        logger.info('Starting stopGraphPipelineInstance task')
        checkRequiredConfig(!configuration?.pipelineinstance?.id)

        logger.debug('Using config [pipelineInstanceId={}]', configuration.pipelineinstance.id)

        Request projectStopPipelineInstanceRequest = saagieUtils.getProjectStopPipelineInstanceRequest()
        tryCatchClosure({
            client.newCall(projectStopPipelineInstanceRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when stoping graph pipeline instance: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    String stoppedPipelineInstanceStatus = parsedResult.data.stopPipelineInstance ? 'success' : 'failure'
                    Map stoppedPipeline = [status: stoppedPipelineInstanceStatus]
                    return JsonOutput.toJson(stoppedPipeline)
                }
            }
        }, 'Unknown error in Task: stopGraphPipelineInstance', 'Function: stopGraphPipelineInstance')
    }

    //TODO voir si on garde, même fonction que pour les pipelines lineaire
    String getGraphPipelineInstanceStatus() {
        logger.info('Starting getGraphPipelineInstanceStatus task')
        checkRequiredConfig(!configuration?.pipelineinstance?.id)

        logger.debug('Using config [pipelineInstanceId={}]', configuration.pipelineinstance.id)
        Request projectPipelineInstanceStatusRequest = saagieUtils.getProjectPipelineInstanceStatusRequest()
        tryCatchClosure({
            client.newCall(projectPipelineInstanceStatusRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when requesting graph pipeline instance status: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map pipelineInstanceStatus = parsedResult.data.pipelineInstance
                    return JsonOutput.toJson(pipelineInstanceStatus)
                }
            }
        }, 'Unknown error in Task: getGraphPipelineInstanceStatus', 'Function: getGraphPipelineInstanceStatus')
    }
}
