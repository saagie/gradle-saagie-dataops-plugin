package io.saagie.plugin.dataops.clients

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Server
import io.saagie.plugin.dataops.utils.SaagieUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static io.saagie.plugin.dataops.DataOpsModule.*

class SaagieClient {
    static final Logger logger = Logging.getLogger(SaagieClient.class)
    static BAD_PROJECT_CONFIG = 'Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/%WIKI%'
    static NO_FILE_MSG = "Check that there is a file to upload in '%FILE%'. Be sure that '%FILE%' is a correct file path."

    DataOpsExtension configuration

    SaagieUtils saagieUtils

    OkHttpClient client = new OkHttpClient()

    JsonSlurper slurper = new JsonSlurper()

    SaagieClient(DataOpsExtension configuration) {
        this.configuration = configuration

        // TODO: remember to parameterize that once it will be available
        this.configuration.jobVersion.resources {
            disk = 512
            memory = 512
            cpu = 0.3
        }

        saagieUtils = new SaagieUtils(configuration)

        this.checkBaseConfiguration()
    }

    private checkBaseConfiguration() {
        logger.debug('Checking for a valid server configuration')
        Server server = configuration.server
        if (server?.url == null ||
            server?.environment == null ||
            server?.login == null ||
            server?.password == null
        ) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_TASK))
        }

        server.url = server.url.trim()
        if (server.url.endsWith('/')) {
            server.url = server.url.substring(0, server.url.length() - 1)
        }

        logger.debug('Server config is valid !')

        if (server?.jwt) {
            if (server?.realm == null) {
                String urlWithoutPrefix = server.url.split('//')[1]
                String customerRealm = urlWithoutPrefix.split('-')[0]
                configuration.server.realm = customerRealm
            }
            configuration.server.realm = configuration.server.realm.toUpperCase()
            logger.debug('Using {} as customer realm', configuration.server.realm)

            logger.debug('Starting jwt authentication')
            Request getJwtTokenRequest = saagieUtils.getJwtTokenRequest()
            client.newCall(getJwtTokenRequest).execute().withCloseable { response ->
                handleErrors(response)
                logger.debug('Successfully authenticated')
                String jwtToken = response.body().string()
                configuration.server.token = jwtToken
            }
        }
    }

    String getProjects() {
        logger.info('Starting getProject task')
        Request projectsRequest = saagieUtils.getProjectsRequest();
        try {
            client.newCall(projectsRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when getting projectList: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    List projects = parsedResult.data.projects
                    return JsonOutput.toJson(projects)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in getProjects')
            throw exception
        }
    }

    String getProjectJobs() {
        logger.info('Starting getProjectJob task')
        if (!configuration.project?.id) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_JOBS_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_JOBS_TASK))
        }

        logger.debug('Using config [project={}]', configuration.project)

        Request projectJobsRequest = saagieUtils.getProjectJobsRequest()
        try {
            client.newCall(projectJobsRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when getting project jobs: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    List jobs = parsedResult.data.jobs
                    return JsonOutput.toJson(jobs)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in getProjectJobs')
            throw exception
        }
    }

    String getProjectTechnologies() {
        logger.info('Starting getProjectTechnologies task')
        if (configuration?.project?.id == null) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_TECHNOLOGIES_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_TECHNOLOGIES_TASK))
        }

        logger.debug('Using config [project={}]', configuration.project)

        Request projectTechnologiesRequest = saagieUtils.getProjectTechnologiesRequest()
        try {
            client.newCall(projectTechnologiesRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when getting project technologies.\n${responseBody}"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    List technologies = parsedResult.data.technologies
                    List uniqueTechnologies = technologies.inject([], { uniqueIds, technology ->
                        if (uniqueIds.any { technology.id == it.id }) {
                            return uniqueIds
                        } else {
                            return uniqueIds << technology
                        }
                    })
                    return JsonOutput.toJson(uniqueTechnologies)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in getProjectTechnologies')
            throw exception
        }
    }

    @Deprecated
    String createProjectJob() {
        logger.info('Starting createProjectJob task')
        if (configuration?.project?.id == null ||
            configuration?.job?.name == null ||
            configuration?.job?.technology == null ||
            configuration?.job?.category == null ||
            configuration?.jobVersion?.runtimeVersion == null ||
            configuration?.jobVersion?.resources == null
        ) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_CREATE_JOB_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_CREATE_JOB_TASK))
        }

        if (configuration.jobVersion.packageInfo?.name != null) {
            File scriptToUpload = new File(configuration.jobVersion.packageInfo.name)
            if (!scriptToUpload.exists()) {
                logger.error(NO_FILE_MSG.replaceAll('%FILE%', configuration.jobVersion.packageInfo.name))
                throw new InvalidUserDataException(NO_FILE_MSG.replaceAll('%FILE%', configuration.jobVersion.packageInfo.name))
            }
        }

        logger.debug('Using config [project={}, job={}, jobVersion={}]', configuration.project, configuration.job, configuration.jobVersion)

        Request projectCreateJobRequest = saagieUtils.getProjectCreateJobRequest()
        try {
            client.newCall(projectCreateJobRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)

                if (parsedResult.data == null) {
                    def message = "Something went wrong when creating project job: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map createdJob = parsedResult.data.createJob
                    String jobId = createdJob.id
                    Request uploadRequest = saagieUtils.getUploadFileToJobRequest(jobId)
                    client.newCall(uploadRequest).execute().withCloseable { uploadResponse ->
                        handleErrors(uploadResponse)
                        String uploadResponseBody = uploadResponse.body().string()
                        def uploadFileParsedResult = slurper.parseText(uploadResponseBody)
                        if (!uploadFileParsedResult) {
                            def message = "Something went wrong when uploading project file job: $uploadResponseBody"
                            logger.error(message)
                            throw new GradleException(message)
                        } else {
                            return JsonOutput.toJson(createdJob)
                        }
                    }
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in createProjectJob')
            throw exception
        }
    }

    String createProjectJobWithGraphQL() {
        logger.info('Starting createProjectJob task')
        if (configuration?.project?.id == null ||
            configuration?.job?.name == null ||
            configuration?.job?.technology == null ||
            configuration?.job?.category == null ||
            configuration?.jobVersion?.runtimeVersion == null ||
            configuration?.jobVersion?.resources == null
        ) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_CREATE_JOB_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_CREATE_JOB_TASK))
        }

        if (configuration.jobVersion.packageInfo?.name != null) {
            File scriptToUpload = new File(configuration.jobVersion.packageInfo.name)
            if (!scriptToUpload.exists()) {
                logger.error(NO_FILE_MSG.replaceAll('%FILE%', configuration.jobVersion.packageInfo.name))
                throw new InvalidUserDataException(NO_FILE_MSG.replaceAll('%FILE%', configuration.jobVersion.packageInfo.name))
            }
        }

        logger.debug('Using config [project={}, job={}, jobVersion={}]', configuration.project, configuration.job, configuration.jobVersion)

        Request projectCreateJobRequest = saagieUtils.getProjectCreateJobRequest()
        try {
            client.newCall(projectCreateJobRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when creating project job: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map createdJob = parsedResult.data.createJob
                    return JsonOutput.toJson(createdJob)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in createProjectJob')
            throw exception
        }
    }

    String runProjectJob() {
        logger.info('Starting runProjectJob task')
        if (configuration?.job?.id == null) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_RUN_JOB_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_RUN_JOB_TASK))
        }

        logger.debug('Using config [job={}]', configuration.job)

        Request runProjectJobRequest = saagieUtils.getRunProjectJobRequest()
        try {
            client.newCall(runProjectJobRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when requesting the job to run: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map runningJob = parsedResult.data.runJob
                    return JsonOutput.toJson(runningJob)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in runProjectJob')
            throw exception
        }
    }

    String updateProjectJob() {
        logger.info('Starting updateProjectJob task')
        if (configuration?.job?.id == null ||
            configuration?.project?.id == null
        ) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_UPDATE_JOB_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_UPDATE_JOB_TASK))
        }

        logger.debug('Using config [project={}, job={}, jobVersion={}]', configuration.project, configuration.job, configuration.jobVersion)

        Request projectUpdateJobRequest = saagieUtils.getProjectUpdateJobRequest()
        try {
            client.newCall(projectUpdateJobRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = 'Something went wrong when updating project job.'
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map updatedJob = parsedResult.data.editJob

                    if (configuration?.jobVersion?.runtimeVersion != null) {
                        Request updateJobVersionRequest = saagieUtils.getAddJobVersionRequest()
                        client.newCall(updateJobVersionRequest).execute().withCloseable { updateResponse ->
                            handleErrors(updateResponse)
                            String updateResponseBody = updateResponse.body().string()
                            def updatedJobVersion = slurper.parseText(updateResponseBody)

                            String newJobVersion = updatedJobVersion.data.addJobVersion.number
                            Request uploadRequest = saagieUtils.getUploadFileToJobRequest(
                                configuration.job.id,
                                newJobVersion
                            )
                            client.newCall(uploadRequest).execute().withCloseable { uploadResponse ->
                                handleErrors(uploadResponse)
                                return JsonOutput.toJson(updatedJob)
                            }
                        }
                    }

                    return JsonOutput.toJson(updatedJob)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in updateProjectJob')
            throw exception
        }
    }

    String createProjectPipelineJob() {
        logger.info('Starting createPipeline task')
        if (configuration?.project?.id == null ||
            configuration?.pipeline?.name == null
        ) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_CREATE_PIPELINE_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_CREATE_PIPELINE_TASK))
        }

        logger.debug('Using config [project={}, pipeline={}, pipelineVersion={}]', configuration.project, configuration.pipeline, configuration.pipelineVersion)

        Request createPipelineRequest = saagieUtils.getCreatePipelineRequest()
        try {
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
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in createPipeline')
            throw exception
        }
    }

    String getJobInstanceStatus() {
        logger.info('Starting getJobInstanceStatus task')
        if (configuration?.jobinstance?.id == null        ) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECTS_GET_JOB_INSTANCE_STATUS))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECTS_GET_JOB_INSTANCE_STATUS))
        }

        logger.debug('Using config [project={}, jobInstance={}]', configuration.project, configuration.jobinstance)

        Request projectJobInstanceStatusRequest = saagieUtils.getProjectJobInstanceStatusRequest()
        try {
            client.newCall(projectJobInstanceStatusRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when requesting job instance status: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map jobInstanceStatus = parsedResult.data.jobInstance
                    return JsonOutput.toJson(jobInstanceStatus)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in getJobInstanceStatus')
            throw exception
        }
    }

    String updateProjectPipeline() {
        logger.info('Starting updateProjectPipeline task')

        // 1. try to update pipeline infos
        if (
            configuration?.pipeline &&
            configuration?.pipeline?.id == null
        ) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_UPDATE_PIPELINE_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_UPDATE_PIPELINE_TASK))
        }
        def pipelineResult = updatePipelineInfos();

        // 2. try to update pipeline version
        if (configuration?.pipelineVersion) {
            if (configuration?.pipelineVersion && configuration?.pipelineVersion?.jobs?.isEmpty()) {
                logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_UPDATE_PIPELINE_TASK))
                throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_UPDATE_PIPELINE_TASK))
            }
            updatePipelineVersion()
        }

        return pipelineResult
    }

    private updatePipelineInfos() {
        logger.debug('Using config [pipeline={}]', configuration.pipeline)
        Request projectUpdatePipelineRequest = saagieUtils.getProjectUpdatePipelineRequest()
        try {
            client.newCall(projectUpdatePipelineRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when updating project pipeline: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map updatedPipeline = parsedResult.data.editPipeline
                    return JsonOutput.toJson(updatedPipeline)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in updateProjectPipeline')
            throw exception
        }
    }

    private updatePipelineVersion() {
        logger.debug('Using config [pipelineVersion={}]', configuration.pipelineVersion)
        Request updatePipelineVersionRequest = saagieUtils.getAddPipelineVersionRequest()
        try {
            client.newCall(updatePipelineVersionRequest).execute().withCloseable { updateResponse ->
                handleErrors(updateResponse)
                String updateResponseBody = updateResponse.body().string()
                def updatedPipelineVersion = slurper.parseText(updateResponseBody)
                if (updatedPipelineVersion.data == null) {
                    def message = "Something went wrong when adding new project pipeline version: $updateResponseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    String newPipelineVersion = updatedPipelineVersion.data.addPipelineVersion.number
                    logger.info('Updated pipelineVersion number: {}', newPipelineVersion)
                }

            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in updateProjectPipeline')
            throw exception
        }
    }

    String getPipelineInstanceStatus() {
        logger.info('Starting getPipelineInstanceStatus task')
        if (configuration?.pipelineInstance?.id == null) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECTS_GET_PIPELINE_INSTANCE_STATUS))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECTS_GET_PIPELINE_INSTANCE_STATUS))
        }

        logger.debug('Using config [pipelineInstanceId={}]', configuration.pipelineInstance.id)
        Request projectPipelineInstanceStatusRequest = saagieUtils.getProjectPipelineInstanceStatusRequest()
        try {
            client.newCall(projectPipelineInstanceStatusRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when requesting pipeline instance status: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map pipelineInstanceStatus = parsedResult.data.pipelineInstance
                    return JsonOutput.toJson(pipelineInstanceStatus)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in getPipelineInstanceStatus')
            throw exception
        }
    }

    String runProjectPipeline() {
        logger.info('Starting runProjectPipeline task')
        if (configuration?.pipeline?.id == null        ) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_RUN_PIPELINE_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_RUN_PIPELINE_TASK))
        }

        logger.debug('Using config [pipelineId={}]', configuration.pipeline.id)

        Request projectRunPipelineRequest = saagieUtils.getProjectRunPipelineRequest()
        try {
            client.newCall(projectRunPipelineRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when running pipeline: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map runPipeline = parsedResult.data.runPipeline
                    return JsonOutput.toJson(runPipeline)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in runProjectPipeline')
            throw exception
        }
    }

    String stopJobInstance() {
        logger.info('Starting stopJobInstance task')
        if (configuration?.jobinstance?.id == null) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_STOP_JOB_INSTANCE_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_STOP_JOB_INSTANCE_TASK))
        }

        logger.debug('Using config [job={}]', configuration.job)

        Request stopJobInstanceRequest = saagieUtils.getStopJobInstanceRequest()
        try {
            client.newCall(stopJobInstanceRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when stopping the job instance: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map stoppedJobInstance = parsedResult.data.stopJobInstance
                    return JsonOutput.toJson(stoppedJobInstance)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in stopJobInstance')
            throw exception
        }
    }

    String deleteProjectPipeline() {
        logger.info('Starting deleteProjectPipeline task')
        if (configuration?.pipeline?.id == null        ) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_DELETE_PIPELINE_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_DELETE_PIPELINE_TASK))
        }

        logger.debug('Using config [pipelineId={}]', configuration.pipeline.id)

        Request projectDeletePipelineRequest = saagieUtils.getProjectDeletePipelineRequest()
        try {
            client.newCall(projectDeletePipelineRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when deleting pipeline: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    String deletedPipelineStatus = parsedResult.data.deletePipeline ? 'success' : 'failure'
                    Map deletedPipeline = [status: deletedPipelineStatus]
                    return JsonOutput.toJson(deletedPipeline)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in deleteProjectPipeline')
            throw exception
        }
    }

    String archiveProjectJob() {
        logger.info('Starting archiveProjectJob task')
        if (configuration?.job?.id == null        ) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_ARCHIVE_JOB_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_ARCHIVE_JOB_TASK))
        }

        logger.debug('Using config [jobId={}]', configuration.job.id)

        Request projectArchiveJobRequest = saagieUtils.getProjectArchiveJobRequest()
        try {
            client.newCall(projectArchiveJobRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.errors) {
                    def message = "Something went wrong when archiving job: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    String archivedJobStatus = parsedResult.data.archiveJob ? 'success' : 'failed'
                    Map archiveJobResult = [status: archivedJobStatus]
                    return JsonOutput.toJson(archiveJobResult)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in archiveProjectJob')
            throw exception
        }
    }

    String stopPipelineInstance() {
        logger.info('Starting stopPipelineInstance task')
        if (configuration?.pipelineInstance?.id == null        ) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECTS_STOP_PIPELINE_INSTANCE))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECTS_STOP_PIPELINE_INSTANCE))
        }

        logger.debug('Using config [pipelineInstanceId={}]', configuration.pipelineInstance.id)

        Request projectStopPipelineInstanceRequest = saagieUtils.getProjectStopPipelineInstanceRequest()
        try {
            client.newCall(projectStopPipelineInstanceRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when stoping pipeline instance: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map stoppedPipeline = parsedResult.data.stopPipelineInstance
                    return JsonOutput.toJson(stoppedPipeline)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in stopPipelineInstance')
            throw exception
        }
    }

    private handleErrors(Response response) {
        logger.debug('Checking server response')
        if (response.successful) {
            logger.debug('No error in server response.')
            return
        }
        String body = response.body().string()

        SaagieUtils.debugResponse(response)

        String status = "${response.code()}"
        def message = "Error $status when requesting on ${configuration.server.url}:\n$body"
        logger.error(message)
        throw new GradleException(message)
    }
}
