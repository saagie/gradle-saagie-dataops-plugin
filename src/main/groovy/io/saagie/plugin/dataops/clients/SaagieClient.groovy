package io.saagie.plugin.dataops.clients

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.EnvVarScopeTypeEnum
import io.saagie.plugin.dataops.models.ExportJobs
import io.saagie.plugin.dataops.models.ExportPipeline
import io.saagie.plugin.dataops.models.ExportVariables
import io.saagie.plugin.dataops.models.Job
import io.saagie.plugin.dataops.models.JobVersion
import io.saagie.plugin.dataops.models.ResponseStatusEnum
import io.saagie.plugin.dataops.models.Server
import io.saagie.plugin.dataops.models.Pipeline
import io.saagie.plugin.dataops.models.PipelineVersion
import io.saagie.plugin.dataops.models.VariableEnvironmentV1DTO
import io.saagie.plugin.dataops.models.VariableEnvironmentV2DTO
import io.saagie.plugin.dataops.tasks.projects.enums.JobV1Category
import io.saagie.plugin.dataops.tasks.projects.enums.JobV1Type
import io.saagie.plugin.dataops.tasks.service.TechnologyService
import io.saagie.plugin.dataops.tasks.service.VariableService
import io.saagie.plugin.dataops.tasks.service.exportTask.ExportContainer
import io.saagie.plugin.dataops.tasks.service.importTask.ImportJobService
import io.saagie.plugin.dataops.tasks.service.importTask.ImportPipelineService
import io.saagie.plugin.dataops.tasks.service.importTask.ImportVariableService
import io.saagie.plugin.dataops.utils.HttpClientBuilder
import io.saagie.plugin.dataops.utils.SaagieUtils
import io.saagie.plugin.dataops.utils.ZipUtils
import io.saagie.plugin.dataops.utils.directory.FolderGenerator
import io.saagie.plugin.dataops.utils.directory.ZippingFolder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

class SaagieClient {
    static final Logger logger = Logging.getLogger(SaagieClient.class)
    static BAD_PROJECT_CONFIG = 'Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/%WIKI%'
    static NO_FILE_MSG = "Check that there is a file to upload in '%FILE%'. Be sure that '%FILE%' is a correct file path."

    DataOpsExtension configuration

    String taskName

    SaagieUtils saagieUtils

    OkHttpClient client

    static JsonSlurper slurper = new JsonSlurper()

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory()

    Validator validator = factory.getValidator()

    String sl = File.separator

    SaagieClient(DataOpsExtension configuration, String taskName) {
        this.configuration = configuration
        this.taskName = taskName

        // TODO: remember to parameterize that once it will be available
        this.configuration.jobVersion.resources {
            disk = 512
            memory = 512
            cpu = 0.3
        }

        saagieUtils = new SaagieUtils(configuration)
        client = HttpClientBuilder.getHttpClient(configuration)

        this.checkBaseConfiguration()
    }

    private checkBaseConfiguration() {
        logger.debug('Checking for a valid server configuration')
        Server server = configuration.server

        Set<ConstraintViolation<Server>> serverViolations = validator.validate(server)

        if (!serverViolations.isEmpty()) {
            for (ConstraintViolation<Server> violation : serverViolations) {
                logger.error(violation.getMessage());
            }
            logger.error('Missing required params in plugin configuration, check that you have url, environment, login and password defined in your server object.')
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', taskName))
        }

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
        tryCatchClosure({
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
        }, 'Unknown error in Task: projectsList', 'Function: getProjects')
    }

    String getProjectJobs() {
        logger.info('Starting getProjectJob task')
        checkRequiredConfig(!configuration.project?.id)

        logger.debug('Using config [project={}]', configuration.project)

        Request projectJobsRequest = saagieUtils.getProjectJobsRequest()
        tryCatchClosure({
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
        }, 'Unknown error in Task: getProjectJobs', 'getProjectJobs')
    }

    String getProjectTechnologies() {
        logger.info('Starting getProjectTechnologies task')
        checkRequiredConfig(!configuration?.project?.id)

        logger.debug('Using config [project={}]', configuration.project)

        Request projectTechnologiesRequest = saagieUtils.getProjectTechnologiesRequest()
        tryCatchClosure({
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
        }, 'Unknown error in Task: getProjectTechnologies', 'getProjectTechnologies')
    }

    @Deprecated
    String createProjectJob(Job job, JobVersion jobVersion) {
        logger.info('Starting deprecated createProjectJob task')
        checkRequiredConfigForJobAndJobVersionAndProjectId(job, jobVersion)

        if (jobVersion.packageInfo?.name != null) {
            File scriptToUpload = new File(jobVersion.packageInfo.name)
            if (!scriptToUpload.exists()) {
                logger.error(NO_FILE_MSG.replaceAll('%FILE%', jobVersion.packageInfo.name))
                throw new InvalidUserDataException(NO_FILE_MSG.replaceAll('%FILE%', jobVersion.packageInfo.name))
            }
        }

        logger.debug('Using config [project={}, job={}, jobVersion={}]', configuration.project, job, jobVersion)

        Request projectCreateJobRequest = saagieUtils.getProjectCreateJobRequest(job, jobVersion)
        tryCatchClosure({
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
                    uploadIfJobHavePackage(jobId)
                    return JsonOutput.toJson(createdJob)
                }
            }
        }, 'Unknown error in deprecated Task: createProjectJob', 'Function: createProjectJob')
    }

    void checkRequiredConfigForJobAndJobVersionAndProjectId(Job job, JobVersion jobVersion) {
        checkRequiredConfig(
                !configuration?.project?.id ||
                        !job?.name ||
                        !job?.technology ||
                        !job?.category ||
                        !jobVersion?.resources
        )
    }

    void uploadIfJobHavePackage(jobId) {
        if (configuration?.jobVersion?.packageInfo?.name) {
            Request uploadRequest = saagieUtils.getUploadFileToJobRequest(jobId)
            client.newCall(uploadRequest).execute().withCloseable { uploadResponse ->
                handleErrors(uploadResponse)
                String uploadResponseBody = uploadResponse.body().string()
                def uploadFileParsedResult = slurper.parseText(uploadResponseBody)
                if (!uploadFileParsedResult) {
                    def message = "Something went wrong when uploading project file job: $uploadResponseBody"
                    logger.error(message)
                    throw new GradleException(message)
                }
            }
        }
    }

    String createProjectJobWithOrWithoutFile(Job job, JobVersion jobVersion) {
        if (jobVersion?.packageInfo?.name != null) {
            createProjectJobWithFile(job, jobVersion)
        } else {
            createProjectJob(job, jobVersion)
        }
    }

    String createProjectJobWithFile(Job job, JobVersion jobVersion) {
        logger.info('Starting createProjectJob task')
        checkRequiredConfigForJobAndJobVersionAndProjectId(job, jobVersion)

        if (jobVersion.packageInfo?.name != null) {
            File scriptToUpload = new File(jobVersion.packageInfo.name)
            if (!scriptToUpload.exists()) {
                logger.error(NO_FILE_MSG.replaceAll('%FILE%', jobVersion.packageInfo.name))
                throw new InvalidUserDataException(NO_FILE_MSG.replaceAll('%FILE%', jobVersion.packageInfo.name))
            }
        }

        logger.debug('Using config [project={}, job={}, jobVersion={}]', configuration.project, job, jobVersion)

        Request projectCreateJobRequest = saagieUtils.getProjectCreateJobRequestWithGraphQL(job, jobVersion)
        tryCatchClosure({
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
                    job.id = createdJob.id
                    return JsonOutput.toJson(createdJob)
                }
            }
        }, 'Unknown error in Task: createProjectJob', 'Function: createProjectJobWithGraphQL')
    }

    String runProjectJob() {
        logger.info('Starting runProjectJob task')
        checkRequiredConfig(!configuration?.job?.id)

        logger.debug('Using config [job={}]', configuration.job)

        Request runProjectJobRequest = saagieUtils.getRunProjectJobRequest()
        tryCatchClosure({
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
        }, 'Unknown error in Task: runProjectJob', 'Function: runProjectJob')
    }

    @Deprecated
    String updateProjectJob() {
        logger.info('Starting updateProjectJob task')
        checkRequiredConfig(!configuration?.job?.id || !configuration?.project?.id)

        logger.debug('Using config [project={}, job={}, jobVersion={}]', configuration.project, configuration.job, configuration.jobVersion)

        Request projectUpdateJobRequest = saagieUtils.getProjectUpdateJobRequest()
        tryCatchClosure({
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
        }, 'Unknown error in Task: updateProjectJob', 'Function: updateProjectJob')
    }

    String upgradeProjectJobWithGraphQL() {
        logger.info('Starting upgradeProjectJob task')
        logger.debug('Using config [job={}, jobVersion={}]', configuration.job, configuration.jobVersion)
        Request projectsUpdateJobRequest = saagieUtils.getProjectUpdateJobFromDataRequest()
        updateProjectJobWithGraphQLFromRequest(projectsUpdateJobRequest)
        def updatedJobVersion = addJobVersion(configuration.job, configuration.jobVersion)
        Map upgradeStatus = [status: 'success', version: updatedJobVersion]
        return JsonOutput.toJson(upgradeStatus)
    }

    String updateProjectJobWithGraphQLFromRequest(Request request) {
        checkRequiredConfig(
                !configuration?.job?.id ||
                        (configuration?.job?.isScheduled && !configuration?.job?.cronScheduling) ||
                        (configuration?.jobVersion?.exists() && !configuration?.jobVersion?.runtimeVersion)
        )
        String updateProjectJobWithGraphQLFromRequestResult = null
        tryCatchClosure({
            client.newCall(request).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when updating project job: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map updatedJob = parsedResult.data.editJob
                    updateProjectJobWithGraphQLFromRequestResult = JsonOutput.toJson(updatedJob)
                }
            }
            return updateProjectJobWithGraphQLFromRequestResult
        }, 'Unknown error in updateProjectJobWithGraphQLFromRequest')

    }


    String addJobVersion(job, jobVersion) {
        // 2. add jobVersion id there is a jobVersion config
        if (jobVersion?.exists()) {
            Request addJobVersionRequest
            if (jobVersion.packageInfo?.name) {
                addJobVersionRequest = saagieUtils.getAddJobVersionRequestWithGraphQL(job, jobVersion)
            } else {
                if (jobVersion.packageInfo?.downloadUrl) {
                    jobVersion.usePreviousArtifact = true
                }
                addJobVersionRequest = saagieUtils.getAddJobVersionRequestWithoutFile(job, jobVersion)
            }
            client.newCall(addJobVersionRequest).execute().withCloseable { updateResponse ->
                handleErrors(updateResponse)
                String updateResponseBody = updateResponse.body().string()
                def updatedJobVersion = slurper.parseText(updateResponseBody)
                if (updatedJobVersion.data == null) {
                    def message = "Something went wrong when adding new job version: $updateResponseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    String newJobVersion = updatedJobVersion.data.addJobVersion.number
                    logger.info('Added new version: {}', newJobVersion)
                    return newJobVersion
                }
            }
        } else if (job?.id) {
            Request listJobVersion = saagieUtils.getListVersionForJobRequest(job?.id)
            client.newCall(listJobVersion).execute().withCloseable { listJobVersionsResponse ->
                handleErrors(listJobVersionsResponse)
                String listJobVersionResponseBody = listJobVersionsResponse.body().string()
                def listJobVersionsData = slurper.parseText(listJobVersionResponseBody)
                if (listJobVersionsData.data == null) {
                    def message = "Something went wrong when getting list job versions: $listJobVersionResponseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    logger.info('getting list job versions: {}', listJobVersionsData.data.job.versions)
                    String currentNumber
                    listJobVersionsData.data.job.versions.each {
                        if (it.isCurrent) {
                            currentNumber = it.number
                        }
                    }
                    return currentNumber
                }
            }
        }
    }

    String createProjectPipeline(Pipeline pipeline, PipelineVersion pipelineVersion) {
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

    String getJobInstanceStatus() {
        logger.info('Starting getJobInstanceStatus task')
        checkRequiredConfig(!configuration?.jobinstance?.id)

        logger.debug('Using config [project={}, jobInstance={}]', configuration.project, configuration.jobinstance)

        Request projectJobInstanceStatusRequest = saagieUtils.getProjectJobInstanceStatusRequest()
        tryCatchClosure({
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
        }, 'Unknown error in getJobInstanceStatus')
    }

    String updateProjectPipeline() {
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

    private updatePipelineInfos() {
        logger.debug('Using config [pipeline={}]', configuration.pipeline)
        Request projectUpdatePipelineRequest = saagieUtils.getProjectUpdatePipelineRequest()
        tryCatchClosure({
            client.newCall(projectUpdatePipelineRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when updating project pipeline: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    String updatedPipelineStatus = parsedResult.data.editPipeline ? 'success' : 'failure'
                    Map updatedPipeline = [status: updatedPipelineStatus]
                    return JsonOutput.toJson(updatedPipeline)
                }
            }
        }, 'Unknown error in updateProjectPipeline')
    }

    private updatePipelineVersion(Pipeline pipeline, PipelineVersion pipelineVersion) {
        logger.debug('Calling updatePipelineVersion')
        logger.debug('Using config [pipeline={}]', pipeline)
        logger.debug('Using config [pipelineVersion={}]', pipelineVersion)
        Request updatePipelineVersionRequest = saagieUtils.getAddPipelineVersionRequest(pipeline, pipelineVersion)
        tryCatchClosure({
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
        }, 'Unknown error in updatePipelineVersion')
    }

    String getPipelineInstanceStatus() {
        logger.info('Starting getPipelineInstanceStatus task')
        checkRequiredConfig(!configuration?.pipelineinstance?.id)

        logger.debug('Using config [pipelineInstanceId={}]', configuration.pipelineinstance.id)
        Request projectPipelineInstanceStatusRequest = saagieUtils.getProjectPipelineInstanceStatusRequest()
        tryCatchClosure({
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
        }, 'Unknown error in Task: getPipelineInstanceStatus', 'Function: getPipelineInstanceStatus')
    }

    String runProjectPipeline() {
        logger.info('Starting runProjectPipeline task')
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
                    def message = "Something went wrong when running pipeline: $responseBody"
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
                        def message = "Something went wrong when intance pipeline: $responseBody"
                        logger.error(message)
                        throw new GradleException(message)
                    } else {
                        Map instancePipelineData = parsedResult.data
                        Map updatedPipeline = [runPipeline: [id: runPipelineData.runPipeline.id, status: instancePipelineData.pipelineInstance.status]]
                        return JsonOutput.toJson(updatedPipeline)
                    }
                }
            } else {
                def message = "Something went wrong when intance pipeline"
                logger.error(message)
                throw new GradleException(message)
            }
        }, 'Unknown error in Task: runProjectPipeline', 'Function: runProjectPipeline')
    }

    String stopJobInstance() {
        logger.info('Starting stopJobInstance task')
        checkRequiredConfig(!configuration?.jobinstance?.id)

        logger.debug('Using config [job={}]', configuration.job)

        Request stopJobInstanceRequest = saagieUtils.getStopJobInstanceRequest()
        tryCatchClosure({
            client.newCall(stopJobInstanceRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when stopping the job instance: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    String stoppedJobInstanceStatus = parsedResult.data.stopJobInstance ? 'success' : 'failure'
                    Map stoppedJobInstance = [status: stoppedJobInstanceStatus]
                    return JsonOutput.toJson(stoppedJobInstance)
                }
            }
        }, 'Unknown error in Task: stopJobInstance', 'Function: stopJobInstance')
    }

    String deleteProjectPipeline() {
        logger.info('Starting deleteProjectPipeline task')
        checkRequiredConfig(!configuration?.pipeline?.id)

        logger.debug('Using config [pipelineId={}]', configuration.pipeline.id)

        Request projectDeletePipelineRequest = saagieUtils.getProjectDeletePipelineRequest()
        tryCatchClosure({
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
        }, 'Unknown error in Task: deleteProjectPipeline', 'Function : deleteProjectPipeline')
    }

    String deleteProject() {
        logger.info('Starting deleteProject task')
        if (configuration?.project?.id == null) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', taskName))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', taskName))
        }

        logger.debug('Using config [projectId={}]', configuration.project.id)

        Request projectDeleteRequest = saagieUtils.archiveProjectRequest()
        tryCatchClosure({
            client.newCall(projectDeleteRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when deleting project: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    String deletedProjectStatus = parsedResult.data.archiveProject ? 'success' : 'failure'
                    Map deletedProject = [status: deletedProjectStatus]
                    return JsonOutput.toJson(deletedProject)
                }
            }
        }, 'Unknown error in Task: deleteProject', 'Function: deleteProject')
    }

    String deleteProjectJob() {
        logger.info('Starting deleteProjectJob task')
        checkRequiredConfig(!configuration?.job?.id)

        logger.debug('Using config [jobId={}]', configuration.job.id)

        Request projectArchiveJobRequest = saagieUtils.getProjectArchiveJobRequest()
        tryCatchClosure({
            client.newCall(projectArchiveJobRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.errors) {
                    def message = "Something went wrong when deleting job: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    String archivedJobStatus = parsedResult.data.archiveJob ? 'success' : 'failed'
                    Map archiveJobResult = [status: archivedJobStatus]
                    return JsonOutput.toJson(archiveJobResult)
                }
            }
        }, 'Unknown error in task : deleteProjectJob', 'Function: deleteProjectJob')
    }

    def saveEnvironmentVariable(environmentVariable) {
        logger.info('saveEnvironmentVariable')
        def requestSaveEnvironmentVariable = null
        def isProjectRequest = environmentVariable.scope.equals(EnvVarScopeTypeEnum.project.name().toUpperCase())
        if (isProjectRequest) {
            requestSaveEnvironmentVariable = saagieUtils.saveProjectEnvironmentVariable(environmentVariable)
        } else {
            requestSaveEnvironmentVariable = saagieUtils.saveGlobalEnvironmentVariable(environmentVariable)
        }
        tryCatchClosure({
            client.newCall(requestSaveEnvironmentVariable).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)

                if (parsedResult.errors) {
                    def message = "Something went wrong when saving environment variable: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    logger.debug("saved environment variable : $responseBody")
                    return parsedResult.data.saveEnvironmentVariable

                }
            }
        }, 'Unknown error in task : importJob', 'Function: saveEnvironmentVariable')
    }

    String stopPipelineInstance() {
        logger.info('Starting stopPipelineInstance task')
        checkRequiredConfig(!configuration?.pipelineinstance?.id)

        logger.debug('Using config [pipelineInstanceId={}]', configuration.pipelineinstance.id)

        Request projectStopPipelineInstanceRequest = saagieUtils.getProjectStopPipelineInstanceRequest()
        tryCatchClosure({
            client.newCall(projectStopPipelineInstanceRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when stoping pipeline instance: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    String stoppedPipelineInstanceStatus = parsedResult.data.stopPipelineInstance ? 'success' : 'failure'
                    Map stoppedPipeline = [status: stoppedPipelineInstanceStatus]
                    return JsonOutput.toJson(stoppedPipeline)
                }
            }
        }, 'Unknown error in Task: stopPipelineInstance', 'Function: stopPipelineInstance')
    }

    String listPlatforms() {
        logger.info('Starting platformList task')

        checkRequiredConfig(
                !configuration.server?.jwt ||
                        !configuration.server?.realm
        )
        Request platformListRequest = saagieUtils.getPlatformListRequest()
        tryCatchClosure({
            client.newCall(platformListRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.authorizations == null) {
                    def message = "Something went wrong when getting platform list: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    List platformListResult = parsedResult.authorizations
                    return JsonOutput.toJson(platformListResult)
                }
            }
        }, 'Unknown error in Task: platformList', 'Function: listPlatforms')
    }

    String listAllPipelines() {
        logger.info('Starting projectsListAllPipelines task')
        checkRequiredConfig(!configuration?.project?.id)

        Request platformListRequest = saagieUtils.getListAllPipelinesRequest()
        tryCatchClosure({
            client.newCall(platformListRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when getting pipelines list: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    List pipelineList = parsedResult.data.pipelines
                    return JsonOutput.toJson(pipelineList)
                }
            }
        }, 'Unknown error in Task: projectsListAllPipelines', 'Function: listAllPipelines')
    }

    String listTechnologies() {
        logger.info('Starting technologyList task')

        Request technologyListRequest = saagieUtils.getListAllTechnologiesRequest()
        tryCatchClosure({
            client.newCall(technologyListRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when getting technology list: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    List technologyList = parsedResult.data.technologies
                    return JsonOutput.toJson(technologyList)
                }
            }
        }, 'Unknown error in Task: technologyList', 'Function: listTechnologies')
    }

    String listGroups() {
        logger.info('Starting groupList task')

        checkRequiredConfig(
                !configuration.server?.jwt ||
                        !configuration.server?.realm || !configuration.server?.environment
        )
        tryCatchClosure({
            Request groupListRequest = saagieUtils.getGroupListRequest()
            client.newCall(groupListRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.groups == null) {
                    def message = "Something went wrong when getting group list: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    List groupListResult = parsedResult.name
                    return JsonOutput.toJson(groupListResult)
                }
            }
        }, 'Unknown error in Task: groupList', 'Function: listGroups')
    }

    String createProject() {
        logger.info('Starting projectsCreate task')

        checkRequiredConfig(!configuration?.project?.name)
        tryCatchClosure({
            Request projectsCreateRequest = saagieUtils.getProjectsCreateRequest()
            client.newCall(projectsCreateRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when creating project: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map createdProjectResult = parsedResult.data.createProject
                    return JsonOutput.toJson(createdProjectResult)
                }
            }
        }, 'Unknown error in Task : createProject', 'Function: createProject')
    }

    String exportArtifactsV1() {
        logger.debug('Starting Export artifacts v1 task')

        checkRequiredConfig(
                !configuration?.exportArtifacts?.export_file
        )

        ExportPipeline[] exportPipelines = []
        ArrayList listJobsByNameAndIdFromV1 = []

        if (configuration.pipeline.ids) {
            exportPipelines = getListPipelineAndpipelineVersionsFromConfigV1(listJobsByNameAndIdFromV1)
        }

        ExportJobs[] exportJobs = []
        if (configuration.job.ids) {
            exportJobs = getListJobAndJobVersionsFromConfigV1(listJobsByNameAndIdFromV1)
        }

        boolean variablesExportedIsEmpty = false
        ExportVariables[] exportVariables
        (exportVariables, variablesExportedIsEmpty) = getVariableListIfConfigIsDefined(this.&getListVariablesV1FromConfig)

        return export(exportPipelines, exportJobs, exportVariables, listJobsByNameAndIdFromV1, variablesExportedIsEmpty, true)
    }

    String exportArtifactsV2() {
        logger.debug('Starting artifacts Job task')
        checkRequiredConfig(!configuration?.project?.id ||
                !configuration?.exportArtifacts?.export_file
        )

        ExportPipeline[] exportPipelines = []
        if (configuration.pipeline.ids) {
            exportPipelines = getListPipelineAndPipelineVersionsFromConfig()
        }

        ExportJobs[] exportJobs = []
        if (configuration.job.ids) {
            exportJobs = getListJobAndJobVersionsFromConfig()
        }

        ExportVariables[] exportVariables = []
        boolean variablesExportedIsEmpty = false
        if (configuration.env.scope) {
            (exportVariables, variablesExportedIsEmpty) = getVariableListIfConfigIsDefined(this.&getListVariablesV2FromConfig)
        }

        return export(exportPipelines, exportJobs, exportVariables, variablesExportedIsEmpty)
    }

    def getVariableListIfConfigIsDefined(getVariableListingClosure) {
        def exportVariables = []
        boolean variablesExportedIsEmpty = false
        def validateVariableConfigurationParams = configuration.env.include_all_var || (configuration.env.name && configuration.env.name.size())
        if (validateVariableConfigurationParams) {
            (exportVariables, variablesExportedIsEmpty) = getVariableListingClosure()
        }
        return [exportVariables, variablesExportedIsEmpty]
    }

    String export(ExportPipeline[] exportPipelines, ExportJobs[] exportJobs, ExportVariables[] exportVariables, listJobWithNameAndIdV1 = null, boolean variablesExportedIsEmpty, isV1 = false) {

        ExportContainer exportContainer = [configuration]
        boolean customDirectoryExist = false
        def tempJobDirectory = null

        (customDirectoryExist, tempJobDirectory) = getTemporaryFile(configuration.exportArtifacts.temporary_directory, customDirectoryExist)

        FolderGenerator folder = [
                tempJobDirectory.getAbsolutePath(),
                saagieUtils,
                client,
                configuration,
                exportContainer.fileName,
                exportContainer.overwrite
        ]

        tryCatchClosure({
            def listJobs = null
            if (isV1) {
                listJobs = listJobWithNameAndIdV1
            } else if (exportPipelines.size() > 0 || exportJobs.size() > 0) {
                listJobs = getProjectJobsByNameAndId()
            }
            def inputDirectoryToZip = tempJobDirectory.getAbsolutePath() + File.separator + exportContainer.fileName
            folder.exportJobList = exportJobs
            folder.exportPipelineList = exportPipelines
            folder.exportVariableList = exportVariables
            folder.jobList = listJobs
            folder.generateFolderFromParams(variablesExportedIsEmpty)
            ZippingFolder zippingFolder = [exportContainer.exportConfigPath, inputDirectoryToZip, customDirectoryExist]
            zippingFolder.generateZip(tempJobDirectory)

            logger.debug("path after: {}, ", exportContainer.exportConfigPath)

            return JsonOutput.toJson([
                    status    : "success",
                    exportfile: exportContainer.exportConfigPath
            ])
        }, 'Unknown error in export method')
    }

    def getProjectJobsByNameAndId() {
        tryCatchClosure({
            Request jobsListRequest = saagieUtils.getProjectJobsGetNameAndIdRequest()
            client.newCall(jobsListRequest).execute().withCloseable { responseJobList ->
                handleErrors(responseJobList)
                String responseBodyForJobList = responseJobList.body().string()
                def parsedResultForJobList = slurper.parseText(responseBodyForJobList)

                if (parsedResultForJobList.data?.jobs) {
                    return parsedResultForJobList.data.jobs
                }
            }
        }, 'getProjectJobsByNameAndId', 'getProjectJobsRequestGetNameAndId Request')
    }

    static getTemporaryFile(String url, boolean customDirectoryExist) {
        def tempJobDirectory = null


        UUID uuid = UUID.randomUUID()

        if (url) {
            def tempJobDirectoryContainer = new File(url)
            customDirectoryExist = tempJobDirectoryContainer.exists()
            if (!customDirectoryExist) {
                throw new GradleException("Could not find main temporary directory, name : ${tempJobDirectoryContainer.name}, verify again please")
            }
            tempJobDirectory = new File("${url}/artifacts-${uuid.toString()}")
            tempJobDirectory.mkdir()
        }

        if (!customDirectoryExist) {
            tempJobDirectory = File.createTempDir("artifacts-${uuid.toString()}", ".tmp")
        }

        if (tempJobDirectory.equals("/tmp")) {
            throw new GradleException("Cannot name custom temporary directory as the tmp system folder")
        }

        if (tempJobDirectory.canWrite()) {
            logger.debug("Temporary directory is created path and have write access {}", tempJobDirectory.getAbsolutePath())
        } else {
            throw new GradleException("Cannot Write inside temporary directory")
        }

        return [customDirectoryExist, tempJobDirectory]
    }

    ExportJobs getJobAndJobVersionDetailToExport(String jobId) {

        checkRequiredConfig(!configuration?.project?.id ||
                !configuration?.job?.ids ||
                !configuration?.exportArtifacts?.export_file
        )
        tryCatchClosure({
            Request getJobDetail = saagieUtils.getJobDetailRequestFromParam(jobId)
            ExportJobs exportJob = new ExportJobs()
            client.newCall(getJobDetail).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null || parsedResult.data.job == null) {
                    def message = "Something went wrong when getting job detail: $responseBody for job id $jobId"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    def jobDetailResult = parsedResult.data.job
                    exportJob.setJobFromApiResult(jobDetailResult)
                    if (jobDetailResult.versions && !jobDetailResult.versions.isEmpty()) {
                        jobDetailResult.versions.sort { a, b -> a.creationDate <=> b.creationDate }
                        jobDetailResult.versions.each {
                            if (it.isCurrent) {
                                exportJob.setJobVersionFromApiResult(it)
                            }
                            if (it.packageInfo && it.packageInfo.downloadUrl) {
                                exportJob.downloadUrl = it.packageInfo.downloadUrl
                                exportJob.downloadUrlVersion = it.number
                            } else {
                                logger.debug("the is no package info here")
                            }
                        }
                    } else {
                        def messageEmptyVersions = "No versions for the job $jobId"
                        logger.error(messageEmptyVersions)
                        throw new GradleException(messageEmptyVersions)
                    }
                }
            }
            return exportJob
        }, 'Unknown error in getJobAndJobVersionDetailToExport method', 'getJobDetailRequestFromParam request') as ExportJobs
    }

    ExportJobs getJobAndJobVersionDetailToExportV1(String jobId, listJobsByNameAndIds) {

        checkRequiredConfig(
                !configuration?.job?.ids ||
                        !configuration?.exportArtifacts?.export_file
        )

        Request getJobDetailV1 = saagieUtils.getJobDetailRequestFromParamV1 jobId
        ExportJobs exportJob = new ExportJobs()
        exportJob.isV1 = true
        tryCatchClosure({
            getV1Client().newCall(getJobDetailV1).execute().withCloseable { response ->
                handleErrors response
                String responseBody = response.body().string()
                def parsedV1job = slurper.parseText responseBody
                logger.debug("getJobDetailV1 response $responseBody")
                testIfJobV1isValid(parsedV1job)

                def allTechnologies = saagieUtils.&getListAllTechnologiesRequest
                def allTechnologyVersions = saagieUtils.&getListTechnologyVersionsRequest

                TechnologyService.instance.init(
                        client,
                        allTechnologies,
                        allTechnologyVersions,
                        slurper
                )

                def technologyV2 = TechnologyService.instance.getV2TechnologyByV1Name(parsedV1job.capsule_code)
                if (!technologyV2) {
                    throwAndLogError("No technology found from the v1 version to the v2 version")
                }


                //TODO Exc
                def formatedCronFromSchedule = SaagieUtils.convertScheduleV1ToCron(parsedV1job.schedule)

                def current = null

                if (parsedV1job.current) {
                    current = parsedV1job.current
                } else {
                    ArrayList jobV1Versions = parsedV1job.versions as ArrayList
                    jobV1Versions.Sort()
                    jobV1Versions.Reverse()
                    current = jobV1Versions[0]
                }

                if (!current) {
                    throwAndLogError("Current inside the getJob and jobVersionDetail toExport V1 can't be null")
                }


                exportJob.setJobFromV1ApiResult(
                        parsedV1job,
                        technologyV2,
                        formatedCronFromSchedule
                )
                def technologyV2Version = getTechnologyV2MappedInformation(technologyV2.id as String, parsedV1job?.current)

                exportJob.setJobVersionFromV1ApiResult(
                        getRunTimeVersionMapper(technologyV2Version, technologyV2),
                        current
                )

                if (configuration.job.include_all_versions && parsedV1job.versions) {
                    def versions = parsedV1job.versions

                    versions.sort { x, y ->
                        x.number <=> y.number
                    }

                    versions.each { version ->
                        def technologyV2VersionForVersions = getTechnologyV2MappedInformation(technologyV2.id as String, parsedV1job?.current)
                        exportJob.addJobVersionFromV1ApiResult(
                                getRunTimeVersionMapper(technologyV2VersionForVersions, technologyV2),
                                version)
                    }
                }

                if (current.file) {
                    exportJob.downloadUrl = current.file
                    exportJob.downloadUrlVersion = current.number
                }
            }

            return exportJob
        }, 'Unknown error in getJobAndJobVersionDetailToExportV1 method') as ExportJobs
    }

    static Map getRunTimeVersionMapper(technologyV2Version, technologyV2) {
        return technologyV2Version ? technologyV2Version : [version2: technologyV2, technologyV2Version: null] as Map
    }

    static Map getTechnologyV2MappedInformation(String technologyV2Id, version) {
        def versionV1 = null
        def extraTechV1 = [:]
        if (version?.options?.language_version) {
            versionV1 = version.options.language_version
        }

        if (version?.options?.extra_language) {
            extraTechV1.language = version.options.extra_language
        }

        if (version?.options?.extra_version) {
            extraTechV1.version = version.options.extra_version
        }


        def resultTechnologiesVersions = TechnologyService.instance.getTechnologyVersions(technologyV2Id)
        def technologyV2Version = null
        if (resultTechnologiesVersions) {
            technologyV2Version = TechnologyService.instance.getMostRelevantTechnologyVersion(technologyV2Id, versionV1, extraTechV1, resultTechnologiesVersions)
        }

        return technologyV2Version
    }

    boolean testIfJobV1isValid(parsedV1job) {
        if (!parsedV1job.capsule_code) {
            throwAndLogError("Something went wrong when getting job detail from v1 job")
        }

        if ([JobV1Type.jupiter].contains(parsedV1job.capsule_code)) {
            throwAndLogError("JUPYTER type not supported by current job task for job id $jobId")
        }

        if ([JobV1Type.docker].contains(parsedV1job.capsule_code)) {
            if (parsedV1job.capsule_code != JobV1Category.processing) {
                throwAndLogError("Only processing jobs are avaible for docker")
            }
        }

        return true
    }

    ExportPipeline getPipelineAndPipelineVersionDetailToExportV1(String pipelineId, listJobsWithNameAndIdFromV1) {

        checkRequiredConfig(
                !configuration?.pipeline?.ids ||
                        !configuration?.exportArtifacts?.export_file
        )
        tryCatchClosure({
            Request getPipelineDetailV1 = saagieUtils.getPipelineRequestFromParamV1(pipelineId)
            def pipeline = configuration.pipeline
            ExportPipeline exportPipeline = new ExportPipeline()
            getV1Client().newCall(getPipelineDetailV1).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                logger.debug("getPipelineDetailV1 response $responseBody")
                def parsedV1PipelineResult = slurper.parseText(responseBody)
                def pipelineName = parsedV1PipelineResult.name
                if (pipelineName) {
                    if (parsedV1PipelineResult.jobs instanceof Collection && parsedV1PipelineResult.jobs.size() > 0) {
                        parsedV1PipelineResult.jobs.each { element ->
                            testIfJobV1isValid(element)
                        }
                    } else {
                        throwAndLogError("Jobs list must be a not empty ArrayList")
                    }

                    listJobsWithNameAndIdFromV1.addAll(parsedV1PipelineResult.jobs.collect { job ->
                        [
                                id  : job.id,
                                name: job.name
                        ]
                    })

                    if (configuration.pipeline.include_all_versions) {
                        def listInstancesPipelines = getAllInstancePipelineInformation(pipelineId)
                        def mappedListInstancesPipelines = listInstancesPipelines.collect { workflow ->
                            getInstancePipelineDetail(workflow.workflowId as String, workflow.id as String)
                        }
                        mappedListInstancesPipelines.each { pipelineInstance ->
                            pipelineInstance.jobs.each { job ->
                                def existingJob = listJobsWithNameAndIdFromV1.find { jobWithName -> jobWithName.name == job.name }
                                if (!existingJob) {
                                    listJobsWithNameAndIdFromV1.add([
                                            id  : job.id,
                                            name: job.name
                                    ])
                                }
                            }
                        }

                        mappedListInstancesPipelines.collect { workflow ->
                            workflow.jobs = workflow.jobs.id
                            return workflow
                        }
                        mappedListInstancesPipelines.eachWithIndex { item, index ->
                            exportPipeline.addPipelineVersionDtoToVersions(item.jobs, null, null)
                        }
                        if (exportPipeline.versions.size() > 1) {
                            exportPipeline.versions.withIndex().collect { workflow, index ->
                                workflow.number = "${index + 1}"
                                workflow.jobs = workflow.jobs.collect { job ->
                                    [id: job]
                                }
                            }
                        }
                    }

                    if (pipeline.include_job) {
                        if (!listJobsWithNameAndIdFromV1 instanceof ArrayList) {
                            throwAndLogError("listJobsByNameAndIdFromV1 must be a list of type ArrayList")
                        }
                        configuration.job.ids.addAll(listJobsWithNameAndIdFromV1.collect { job -> job.id })

                        if (configuration.pipeline.include_all_versions) {
                            exportPipeline.versions.each {
                                configuration.job.ids.addAll(it.jobs.id)
                            }
                            def listJobIdsInt = configuration.job.ids.collect { it as Integer }
                            configuration.job.ids = listJobIdsInt.unique { a, b -> a <=> b }
                        }
                    }

                    exportPipeline.setPipelineFromV1ApiResult(parsedV1PipelineResult)


                    exportPipeline.setPipelineVersionFromV1ApiResult(
                            parsedV1PipelineResult.jobs.collect { job ->
                                [id: job.id]
                            },
                            parsedV1PipelineResult?.releaseNote
                    )
                }

                return exportPipeline
            }
        }, 'Unknown error in getPipelineAndPipelineVersionDetailToExport method') as ExportPipeline
    }

    ArrayList<?> getAllInstancePipelineInformation(String pipelineId) {
        Request getPipelineInstancesV1Detail = saagieUtils.getPipelineInstancesRequestFromParamV1(pipelineId)
        tryCatchClosure({
            getV1Client().newCall(getPipelineInstancesV1Detail).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                logger.debug("getPipelineInstancesV1Detail response $responseBody")
                def parsedV1PipelineResult = slurper.parseText(responseBody)
                def parsedV1PipelineResultContent = parsedV1PipelineResult.content
                if (parsedV1PipelineResultContent && parsedV1PipelineResultContent.size() > 0) {
                    logger.debug("getPipelineInstancesV1Detail body response $parsedV1PipelineResultContent")
                    return parsedV1PipelineResultContent
                } else {
                    return null
                }
            }
        }, 'Unknown error in getAllInstancePipelineInformation method', 'getPipelineInstancesRequestFromParamV1 request') as ArrayList<?>
    }

    def getInstancePipelineDetail(String pipelineId, String instanceId) {
        Request getPipelineInstanceV1Detail = saagieUtils.getPipelineInstanceDetailRequestFromParamV1(pipelineId, instanceId)
        tryCatchClosure({
            getV1Client().newCall(getPipelineInstanceV1Detail).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                logger.debug("getPipelineInstanceV1Detail response $responseBody")
                def parsedV1PipelineResult = slurper.parseText(responseBody)
                return parsedV1PipelineResult
            }
        }, 'Unknown error in getAllInstancePipelineInformation', 'getPipelineInstanceDetailRequestFromParamV1')

    }

    ExportPipeline getPipelineAndPipelineVersionDetailToExport(String pipelineId) {

        checkRequiredConfig(!configuration?.project?.id ||
                !configuration?.pipeline?.ids ||
                !configuration?.exportArtifacts?.export_file
        )


        tryCatchClosure({
            Request getPipelineDetail = saagieUtils.getPipelineRequestFromParam(pipelineId)
            def pipeline = configuration.pipeline
            ExportPipeline exportPipeline = new ExportPipeline()
            client.newCall(getPipelineDetail).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null || parsedResult.data.pipeline == null) {
                    def message = "Something went wrong when getting pipeline detail: $responseBody for pipeline id $pipelineId"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    def pipelineDetailResult = parsedResult.data.pipeline
                    if (pipelineDetailResult) {
                        exportPipeline.setPipelineFromApiResult(pipelineDetailResult)
                        if (pipelineDetailResult.versions && !pipelineDetailResult.versions.isEmpty()) {
                            pipelineDetailResult.versions.sort { a, b -> a.creationDate <=> b.creationDate }
                            pipelineDetailResult.versions.each {
                                if (it.isCurrent) {
                                    exportPipeline.setPipelineVersionFromApiResult(it)
                                }
                                if (it.jobs && pipeline.include_job) {
                                    def jobIds = configuration.job.ids
                                    configuration.job.ids = [jobIds, it.jobs.id].flatten()
                                }
                            }
                        } else {
                            def messageEmptyVersions = "No versions for the pipeline $pipelineId"
                            logger.error(messageEmptyVersions)
                            throw new GradleException(messageEmptyVersions)
                        }
                    }

                }
            }
            return exportPipeline
        }, 'Unknown error in getPipelineAndPipelineVersionDetailToExport', 'getPipelineRequestFromParam') as ExportPipeline
    }

    ExportPipeline[] getListPipelineAndPipelineVersionsFromConfig() {
        return getPipelineAndPipelineVersions(this.&getPipelineAndPipelineVersionDetailToExport)
    }

    ExportPipeline[] getListPipelineAndpipelineVersionsFromConfigV1(ArrayList listJobsByNameAndIdFromV1) {
        return getPipelineAndPipelineVersions(this.&getPipelineAndPipelineVersionDetailToExportV1, listJobsByNameAndIdFromV1)
    }

    ExportPipeline[] getPipelineAndPipelineVersions(Closure operation, listJobsByNameAndIdFromV1 = null) {
        checkRequiredConfig(
                !configuration?.pipeline?.ids ||
                        !configuration?.exportArtifacts?.export_file
        )
        def listPipelineIds = configuration.pipeline.ids.unique { a, b -> a <=> b }
        def arrayPipelines = []
        listPipelineIds.each { pipelineId ->
            def sPipeLineInd = pipelineId as String
            arrayPipelines.add(listJobsByNameAndIdFromV1 != null ? operation(sPipeLineInd, listJobsByNameAndIdFromV1) : operation(sPipeLineInd))
        }
        return arrayPipelines as ExportPipeline[]
    }

    ExportJobs[] getListJobAndJobVersionsFromConfig() {
        return getListJobAndJobVersions(this.&getJobAndJobVersionDetailToExport)
    }

    ExportJobs[] getListJobAndJobVersionsFromConfigV1(ArrayList listJobsByNameAndIdFromV1) {
        return getListJobAndJobVersions(this.&getJobAndJobVersionDetailToExportV1, listJobsByNameAndIdFromV1)
    }

    ExportJobs[] getListJobAndJobVersions(Closure operation, listJobsByNameAndIdFromV1 = null) {
        checkRequiredConfig(
                !configuration?.job?.ids ||
                        !configuration?.exportArtifacts?.export_file
        )
        def listJobIdsInt = configuration.job.ids.collect { it as String }
        def listJobIds = listJobIdsInt.unique { a, b -> a <=> b }
        def arrayJobs = []

        listJobIds.each { jobId ->
            arrayJobs.add(listJobsByNameAndIdFromV1 != null ? operation(jobId as String, listJobsByNameAndIdFromV1) : operation(jobId as String))
        }
        return arrayJobs as ExportJobs[]
    }

    String callGetJobDetail() {

        checkRequiredConfig(!configuration?.project?.id ||
                !configuration?.job?.id
        )
        tryCatchClosure({
            Request getJobDetail = saagieUtils.getJobDetailRequest()
            client.newCall(getJobDetail).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when getting job detail: $responseBody for job id $configuration.job.id"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map jobDetailResult = parsedResult.data.job
                    return JsonOutput.toJson(jobDetailResult)
                }
            }
        }, 'Unknown error in callGetJobDetail', 'getJobDetailRequest')

    }

    def getListVariablesV2FromConfig() {
        logger.info('Starting getting environment variables from configuration for v2 ... ')
        checkRequiredConfig(checkConfigurationForVariableEnvironmentIsValid())
        boolean variablesExportedIsEmpty = false
        tryCatchClosure({
            Request variablesListRequest = null
            if (configuration.env.scope.equals(EnvVarScopeTypeEnum.global.name())) {
                variablesListRequest = saagieUtils.getGlobalEnvironmentVariables()
            } else {
                variablesListRequest = saagieUtils.getProjectEnvironmentVariables(configuration.project.id)
            }
            def listVariables = getListOfVariablesFromRequest(variablesListRequest)

            if (configuration.env.scope.equals(EnvVarScopeTypeEnum.project.name())) {
                listVariables = listVariables.findAll {
                    it.scope?.equals(EnvVarScopeTypeEnum.project.name().toUpperCase())
                }.collect {
                    it
                }
            }

            if (listVariables.size().equals(0)) {
                def exportVariables1 = []
                logger.warn("WARNING: No environment variable found on the targeted platform with scope ${configuration.env.scope}")
                variablesExportedIsEmpty = true
                return [exportVariables1, variablesExportedIsEmpty]
            }

            if (checkIfEnvDefinedNamesIsValidFromConfiguration()) {
                listVariables = listVariables.findAll {
                    configuration.env.name.contains(it.name)
                }.collect {
                    it
                }
            }


            if (!configuration.env.include_all_var) {
                configuration.env.name.forEach {
                    def foundName = listVariables.find { var -> var.name?.equals(it) }
                    if (!foundName) {
                        throw new GradleException("Didn't find variable name: ${it} in the required environment variables list for scope ${configuration.env.scope.equals(EnvVarScopeTypeEnum.project.name()) ? EnvVarScopeTypeEnum.project.name().toString() : EnvVarScopeTypeEnum.global.name().toString()}")
                    }
                }
            }

            def exportVariables = []

            listVariables.forEach {
                ExportVariables newExportVariable = new ExportVariables()
                newExportVariable.variableEnvironmentDTO = new VariableEnvironmentV2DTO()
                newExportVariable.variableEnvironmentDTO.setVariableDetailValuesFromData(it)
                exportVariables.add(newExportVariable)
            }

            return [exportVariables, variablesExportedIsEmpty]

        }, 'Error in getListVariablesV2FromConfig', 'getGlobalEnvironmentVariables | getProjectEnvironmentVariables Request')

    }

    def getListOfVariablesFromRequest(Request variablesListRequest) {
        def listVariables = []
        client.newCall(variablesListRequest).execute().withCloseable { responseVariableList ->
            handleErrors(responseVariableList)
            String responseBodyForVariableList = responseVariableList.body().string()
            logger.debug("variable list : {}", responseBodyForVariableList)
            def parsedResultForVariableList = slurper.parseText(responseBodyForVariableList)
            if (configuration.env.scope.equals(EnvVarScopeTypeEnum.global.name()) && parsedResultForVariableList.data?.globalEnvironmentVariables) {
                listVariables = parsedResultForVariableList.data.globalEnvironmentVariables
            } else if (parsedResultForVariableList.data?.projectEnvironmentVariables) {
                listVariables = parsedResultForVariableList.data.projectEnvironmentVariables
            }
            return listVariables
        }
    }

    def getListVariablesV1FromConfig() {
        logger.info('Starting getting environment variables from configuration for v1... ')
        checkRequiredConfig(checkConfigurationForVariableEnvironmentIsValid(true))
        boolean variablesExportedIsEmpty = false
        def listVariables = []

        tryCatchClosure({

            listVariables = getAllVariablesFromV1()

            if (!configuration.env.include_all_var && configuration.env.name) {
                configuration.env.name.forEach {
                    def foundName = listVariables.find { var -> var.name?.equals(it) }
                    if (!foundName) {
                        throw new GradleException("Didn't find variable name: ${it} in the required environment variables list in V1")
                    }
                }
                listVariables = listVariables.findAll {
                    configuration.env.name.contains(it.name)
                }.collect {
                    it
                }
            }

            if (listVariables.size().equals(0)) {
                logger.warn("WARNING: No environment variable found on the targeted platform with scope ${configuration.env.scope}")
                variablesExportedIsEmpty = true
                return [[], variablesExportedIsEmpty]
            }

            def exportVariablesV1 = []

            listVariables.collect { element ->
                element.scope = configuration.env.scope.equals(EnvVarScopeTypeEnum.project.name()) ? EnvVarScopeTypeEnum.project.name().toUpperCase() : EnvVarScopeTypeEnum.global.name().toUpperCase()
            }

            listVariables.forEach {
                ExportVariables newExportVariable = new ExportVariables()
                newExportVariable.variableEnvironmentDTO = new VariableEnvironmentV1DTO()
                newExportVariable.variableEnvironmentDTO.setVariableDetailValuesFromData(it)
                exportVariablesV1.add(newExportVariable)
            }

            return [exportVariablesV1, variablesExportedIsEmpty]
        }, 'Error in getListVariablesV2FromConfig', 'getListVariablesV1FromConfig')


    }

    def checkIfEnvDefinedNamesIsValidFromConfiguration() {
        return !configuration.env.include_all_var && configuration.env.name && configuration.env.name.size() > 0
    }

    def getAllVariablesFromV1() {
        checkRequiredConfig(!this.configuration.server.url || !this.configuration.server.environment)
        tryCatchClosure({
            def variablesV1ListRequest = saagieUtils.getAllVariablesFromV1()
            getV1Client().newCall(variablesV1ListRequest).execute().withCloseable { responseVariableList ->
                handleErrors(responseVariableList)
                String responseBodyForVariableList = responseVariableList.body().string()
                logger.debug("variable list : {} ", responseBodyForVariableList)
                return slurper.parseText(responseBodyForVariableList)
            }
        }, 'Error in getListVariablesV1FromConfig', 'getAllVariablesFromV1 Request')

    }

    def checkConfigurationForVariableEnvironmentIsValid(boolean isV1 = false) {
        return checkIfEnvHaveNotScopeOrScopeDifferentFromGlobalAndProject() ||
                checkIfEnvScopeIsProjectButNoProjectIdProvided(isV1) ||
                checkIfOptionIncludeAllVarIsNotSetAndEnvNameAreEmpty()
    }

    def checkIfEnvHaveNotScopeOrScopeDifferentFromGlobalAndProject() {
        def conditionForEnvHaveNotScopeOrScopeDifferentFromGlobalAndProject = (!configuration.env.scope || (!configuration.env.scope.equals(EnvVarScopeTypeEnum.global.name()) && !configuration.env.scope.equals(EnvVarScopeTypeEnum.project.name())))
        logger.debug("result for checkIfEnvHaveNotScopeOrScopeDifferentFromGlobalAndProject")
        logger.debug(conditionForEnvHaveNotScopeOrScopeDifferentFromGlobalAndProject as String)
        return conditionForEnvHaveNotScopeOrScopeDifferentFromGlobalAndProject
    }

    def checkIfEnvScopeIsProjectButNoProjectIdProvided(boolean isV1) {
        def conditionForScopeIsProjectButNoProjectIdProvided = (!isV1 && configuration.env.scope.equals(EnvVarScopeTypeEnum.project.name()) && configuration.project.id.equals(null))
        logger.debug("result for checkIfEnvScopeIsProjectButNoProjectIdProvided")
        logger.debug(conditionForScopeIsProjectButNoProjectIdProvided as String)
        return conditionForScopeIsProjectButNoProjectIdProvided
    }

    def checkIfOptionIncludeAllVarIsNotSetAndEnvNameAreEmpty() {
        def conditionForOptionIncludeAllVarIsNotSetAndEnvNameAreEmpty = (!configuration.env.include_all_var && (!configuration.env.name || !configuration.env.name.size()))
        logger.debug("result for checkIfOptionIncludeAllVarIsNotSetAndEnvNameAreEmpty")
        logger.debug(conditionForOptionIncludeAllVarIsNotSetAndEnvNameAreEmpty as String)
        return conditionForOptionIncludeAllVarIsNotSetAndEnvNameAreEmpty
    }

    String updateProject() {
        logger.info('Starting projectsUpdate task')

        checkRequiredConfig(!configuration?.project?.id)
        tryCatchClosure({
            Request projectsUpdateRequest = saagieUtils.getProjectsUpdateRequest()
            client.newCall(projectsUpdateRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = "Something went wrong when updating project: $responseBody"
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map createdProjectResult = parsedResult.data.editProject
                    return JsonOutput.toJson([
                            status: 'success'
                    ])
                }
            }
        }, 'Unknown error in the Task: projectsUpdate', 'Function: updateProject')
    }

    String importArtifacts() {
        logger.info('Starting importArtifacts task')

        checkRequiredConfig(
                !configuration?.project?.id ||
                        !configuration?.importArtifacts?.import_file
        )

        // Step 1. scan files and create job if needed, based on the existing rules
        String exportedJobFilePath = configuration.importArtifacts.import_file

        File exportedJob = new File(exportedJobFilePath)
        if (!exportedJob.exists() || !exportedJob.canRead()) {
            logger.error(NO_FILE_MSG.replaceAll('%FILE%', exportedJobFilePath))
            throw new InvalidUserDataException(NO_FILE_MSG.replaceAll('%FILE%', exportedJobFilePath))
        }

        def tempFolder = null
        boolean customDirectoryExist = false


        (customDirectoryExist, tempFolder) = getTemporaryFile(configuration.importArtifacts.temporary_directory, customDirectoryExist)

        try {
            try {
                ZipUtils.unzip(exportedJobFilePath, tempFolder.absolutePath)
            } catch (IOException e) {
                logger.error('An error occurred when unzipping the artifacts export file.')
                throw new GradleException(e.message)
            }
            File tempFolderContain = new File(tempFolder.absolutePath).listFiles().head()
            if (tempFolderContain == null) {
                throw new GradleException("Somthing went wrong when trying to access unzipped folder")
            }
            String nameFileFromExportedUrl = saagieUtils.getFileNameFromUrl(tempFolderContain.absolutePath)
            def exportedArtifactsPathRoot = new File("${tempFolder.absolutePath}/${nameFileFromExportedUrl}")
            def jobsConfigFromExportedZip = SaagieClientUtils.extractJobConfigAndPackageFromExportedJob(exportedArtifactsPathRoot)
            def pipelinesConfigFromExportedZip = SaagieClientUtils.extractPipelineConfigAndPackageFromExportedPipeline(exportedArtifactsPathRoot)
            def variablesConfigFromExportedZip = SaagieClientUtils.extractVariableConfigAndPackageFromExportedVariable(exportedArtifactsPathRoot)
            def response = [
                    status  : 'success',
                    job     : [],
                    pipeline: [],
                    variable: []
            ]
            def listJobs = null
            def processJobImportation = { newMappedJobData, job, id, versions = null ->
                def jobToImport = new Job()
                def jobVersionToImport = new JobVersion()
                jobToImport = newMappedJobData.job
                jobVersionToImport = newMappedJobData.jobVersion
                listJobs = getJobListByNameAndId()
                boolean nameExist = false
                def foundNameId = null
                if (listJobs) {
                    listJobs.each {
                        if (it.name == newMappedJobData.job.name) {
                            nameExist = true
                            foundNameId = it.id
                        }
                    }
                }
                def parsedNewlyCreatedJob = null
                // change the job to Queue so we can remove the first
                if (listJobs && nameExist) {
                    jobToImport.id = foundNameId
                    addJobVersion(jobToImport, jobVersionToImport)

                } else {
                    versions = versions as Queue
                    if (versions && versions.size() >= 1) {
                        def firstVersionInV1Format = versions.poll()
                        JobVersion firstVersion = ImportJobService.convertFromMapToJsonVersion(firstVersionInV1Format)
                        jobVersionToImport = firstVersion
                    }
                    def resultCreatedJob = createProjectJobWithOrWithoutFile(jobToImport, jobVersionToImport)
                    parsedNewlyCreatedJob = slurper.parseText(resultCreatedJob)
                }

                response.job << [
                        id  : job.key,
                        name: newMappedJobData.job.name
                ]

                if (versions && versions.size() >= 1) {
                    if (!parsedNewlyCreatedJob?.id && !foundNameId) {
                        throw new GradleException("Couldn't get id for the job after creation or update")
                    }
                    if (parsedNewlyCreatedJob?.id) {
                        jobToImport.id = parsedNewlyCreatedJob?.id
                    } else {
                        jobToImport.id = foundNameId
                    }
                    versions.each {
                        JobVersion jobVersionFromVersions = ImportJobService.convertFromMapToJsonVersion(it)
                        addJobVersion(jobToImport, jobVersionFromVersions)
                    }

                    response.job.last() << [
                            versions: versions.size() + 1
                    ]
                }

            }

            def listPipelines = null
            def processPipelineToImport = { newMappedPipeline, pipeline, id, versions, newlistJobs ->
                listPipelines = getPipelineListByNameAndId()
                def pipelineToImport = newMappedPipeline.pipeline
                def pipelineVersionToImport = newMappedPipeline.pipelineVersion
                boolean nameExist = false
                def pipelineFoundId = null
                if (listPipelines) {
                    listPipelines.each {
                        if (it.name == newMappedPipeline.pipeline.name) {
                            pipelineFoundId = it.id
                            nameExist = true
                        }
                    }
                    // change the job to Queue so we can remove the first
                }

                pipelineToImport.id = pipelineFoundId
                def parsedNewlyCreatedPipeline = null

                if (listPipelines && nameExist) {
                    updatePipelineVersion(pipelineToImport, pipelineVersionToImport)
                } else {
                    if (versions && versions.size() >= 1) {
                        versions = versions as Queue
                        def firstVersionInV1Format = versions.poll()
                        PipelineVersion firstVersion = ImportPipelineService.convertFromMapToJsonVersion(firstVersionInV1Format, newlistJobs)
                        pipelineVersionToImport = firstVersion
                    }

                    def newlyCreatedPipeline = createProjectPipeline(pipelineToImport, pipelineVersionToImport)
                    parsedNewlyCreatedPipeline = slurper.parseText(newlyCreatedPipeline)
                }

                response.pipeline << [
                        id  : pipeline.key,
                        name: newMappedPipeline.pipeline.name
                ]

                if (versions?.size() >= 1) {
                    versions.each {
                        if (!parsedNewlyCreatedPipeline?.id && !pipelineFoundId) {
                            throw new GradleException("Couldn't get id for the pipeline after creation or update")
                        }
                        if (parsedNewlyCreatedPipeline?.id) {
                            pipelineToImport.id = parsedNewlyCreatedPipeline?.id
                        } else {
                            pipelineToImport.id = pipelineFoundId
                        }
                        PipelineVersion pipelineVersionFromVersions = ImportPipelineService.convertFromMapToJsonVersion(it, newlistJobs)
                        updatePipelineVersion(pipelineToImport, pipelineVersionFromVersions)

                    }

                    response.pipeline.last() << [
                            versions: versions.size() + 1
                    ]
                }

            }

            def processVariableToImport = { newMappedVariable, variable, id ->

                def newlyCreatedVariable = saveEnvironmentVariable(newMappedVariable)

                response.variable << [
                        id  : newlyCreatedVariable.id,
                        name: newlyCreatedVariable.name
                ]

            }

            if (jobsConfigFromExportedZip?.jobs) {
                ImportJobService.importAndCreateJobs(
                        jobsConfigFromExportedZip.jobs,
                        configuration,
                        processJobImportation
                )
            }

            if (pipelinesConfigFromExportedZip?.pipelines && response.status == ResponseStatusEnum.success.name) {
                def newlistJobs = getJobListByNameAndId()
                ImportPipelineService.importAndCreatePipelines(
                        pipelinesConfigFromExportedZip.pipelines,
                        configuration,
                        processPipelineToImport,
                        newlistJobs
                )
            }

            if (variablesConfigFromExportedZip?.variables && response.status == ResponseStatusEnum.success.name) {

                ImportVariableService.importAndCreateVariables(
                        variablesConfigFromExportedZip.variables,
                        configuration,
                        processVariableToImport
                )
            }

            return response
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error(exception.message)
            throw exception
        } finally {
            SaagieUtils.cleanDirectory(tempFolder, logger)
        }
    }

    Object[] getVariableEnvironmentFromList(newMappedVariable) {
        Request requestlistVariablesByNameAndId = null

        def isProjectRequest = newMappedVariable.scope.equals(EnvVarScopeTypeEnum.project.name().toUpperCase())
        if (isProjectRequest) {
            requestlistVariablesByNameAndId = saagieUtils.getProjectVariableByNameAndIdAndScope()
        } else {
            requestlistVariablesByNameAndId = saagieUtils.getGlobalVariableByNameAndIdAndScope()
        }
        tryCatchClosure({
            def listVariables = null
            client.newCall(requestlistVariablesByNameAndId).execute().withCloseable { responseVariableList ->
                handleErrors(responseVariableList)
                String responseBodyForVariableList = responseVariableList.body().string()
                logger.debug("variable list : $responseBodyForVariableList")
                def parsedResultForVariableList = slurper.parseText(responseBodyForVariableList)
                if (isProjectRequest) {
                    listVariables = parsedResultForVariableList.data.projectEnvironmentVariables
                } else if (parsedResultForVariableList.data?.globalEnvironmentVariables) {
                    listVariables = parsedResultForVariableList.data.globalEnvironmentVariables
                }
                return listVariables
            }
        }, 'Unknown error in importArtifact Request', 'getVariableEnvironmentFromList Request')
    }

    private getJobListByNameAndId() {
        def listJobs = null
        tryCatchClosure({
            Request jobsListRequest = saagieUtils.getProjectJobsGetNameAndIdRequest()
            client.newCall(jobsListRequest).execute().withCloseable { responseJobList ->
                handleErrors(responseJobList)
                String responseBodyForJobList = responseJobList.body().string()
                logger.debug("Jobs with name and id : ")
                logger.debug(responseBodyForJobList)
                def parsedResultForJobList = slurper.parseText(responseBodyForJobList)
                if (parsedResultForJobList.data?.jobs) {
                    listJobs = parsedResultForJobList.data.jobs
                }
                return listJobs
            }
        }, 'Unknown error in getJobListByNameAndId', 'getProjectJobsGetNameAndIdRequest Request')
    }

    private getPipelineListByNameAndId() {
        def listPipelines = null
        tryCatchClosure({
            Request pipelineListRequest = saagieUtils.getProjectPipelinesRequestGetNameAndId()
            // the job do not exists, create it
            client.newCall(pipelineListRequest).execute().withCloseable { responsePipelineList ->
                handleErrors(responsePipelineList)
                String responseBodyForPipelineList = responsePipelineList.body().string()
                def parsedResultForPipelineList = slurper.parseText(responseBodyForPipelineList)
                if (parsedResultForPipelineList.data?.pipelines) {
                    listPipelines = parsedResultForPipelineList.data.pipelines
                }
                return listPipelines
            }
        }, 'Unknown error in getPipelineListByNameAndId', 'getProjectPipelinesRequestGetNameAndId Request')
    }

    private String parseDataAndReturnJsonOutPut(String data) {
        def jsonResult = slurper.parseText(data)
        if (jsonResult.id) {
            return JsonOutput.toJson([
                    status: 'success',
                    id    : jsonResult.id
            ])
        } else {
            return JsonOutput.toJson([
                    status: 'failed'
            ])
        }
    }

    private checkRequiredConfig(boolean conditions) {
        logger.info('Checking required pre-conditions...')
        if (conditions) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', taskName))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', taskName))
        }
    }

    static handleErrors(Response response) {
        SaagieUtils.handleErrorClosure(logger, response)
    }

    static throwAndLogError(message) {
        SaagieUtils.throwAndLogError(logger, message)
    }

    def getV1Client() {
        HttpClientBuilder.getHttpClientV1(configuration)
    }

    def isArray(array) {
        return array != null && array.getClass().isArray()
    }

    def tryCatchClosure(Closure closure, String message, String potentialFunctionName = null) {
        try {
            closure()
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error(message)
            logger.error("${exception.message} ${potentialFunctionName ?: ''}")
            throw exception
        }
    }
}
