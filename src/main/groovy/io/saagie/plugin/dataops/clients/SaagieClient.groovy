package io.saagie.plugin.dataops.clients

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.ExportJobs
import io.saagie.plugin.dataops.models.ExportPipeline
import io.saagie.plugin.dataops.models.Job
import io.saagie.plugin.dataops.models.JobVersion
import io.saagie.plugin.dataops.models.Server
import io.saagie.plugin.dataops.tasks.projects.enums.JobV1Category
import io.saagie.plugin.dataops.tasks.projects.enums.JobV1Type
import io.saagie.plugin.dataops.tasks.service.TechnologyService
import io.saagie.plugin.dataops.tasks.service.exportTask.ExportContainer
import io.saagie.plugin.dataops.tasks.service.importTask.ImportJobService
import io.saagie.plugin.dataops.tasks.service.importTask.ImportPipelineService
import io.saagie.plugin.dataops.utils.HttpClientBuilder
import io.saagie.plugin.dataops.utils.SaagieUtils
import io.saagie.plugin.dataops.utils.ZipUtils
import io.saagie.plugin.dataops.utils.directory.FolderGenerator
import io.saagie.plugin.dataops.utils.directory.ZippingFolder
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.ssl.SSLContexts

import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

class SaagieClient {
    static final Logger logger = Logging.getLogger(SaagieClient.class)
    static BAD_PROJECT_CONFIG = 'Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/%WIKI%'
    static NO_FILE_MSG = "Check that there is a file to upload in '%FILE%'. Be sure that '%FILE%' is a correct file path."

    DataOpsExtension configuration

    String taskName

    SaagieUtils saagieUtils

    OkHttpClient client

    JsonSlurper slurper = new JsonSlurper()

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
        checkRequiredConfig(!configuration.project?.id)

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
        checkRequiredConfig(!configuration?.project?.id)

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
        logger.info('Starting deprecated createProjectJob task')
        checkRequiredConfig(
            !configuration?.project?.id ||
                !configuration?.job?.name ||
                !configuration?.job?.technology ||
                !configuration?.job?.category ||
                !configuration?.jobVersion?.runtimeVersion ||
                !configuration?.jobVersion?.resources
        )

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
                    uploadIfJobHavePackage(jobId)
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

    void uploadIfJobHavePackage (jobId) {
        if(configuration?.jobVersion?.packageInfo?.name) {
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

    String createProjectJobWithOrWithFile(){
        if (configuration.jobVersion?.packageInfo?.name != null) {
            createProjectJobWithGraphQL()
        }else{
            createProjectJob()
        }
    }

    String createProjectJobWithGraphQL() {
        logger.info('Starting createProjectJob task')
        checkRequiredConfig(
            !configuration?.project?.id ||
                !configuration?.job?.name ||
                !configuration?.job?.technology ||
                !configuration?.job?.category ||
                !configuration?.jobVersion?.runtimeVersion ||
                !configuration?.jobVersion?.resources
        )

        if (configuration.jobVersion.packageInfo?.name != null) {
            File scriptToUpload = new File(configuration.jobVersion.packageInfo.name)
            if (!scriptToUpload.exists()) {
                logger.error(NO_FILE_MSG.replaceAll('%FILE%', configuration.jobVersion.packageInfo.name))
                throw new InvalidUserDataException(NO_FILE_MSG.replaceAll('%FILE%', configuration.jobVersion.packageInfo.name))
            }
        }

        logger.debug('Using config [project={}, job={}, jobVersion={}]', configuration.project, configuration.job, configuration.jobVersion)

        Request projectCreateJobRequest = saagieUtils.getProjectCreateJobRequestWithGraphQL()
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
                    configuration.job.id = createdJob.id
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
        checkRequiredConfig(!configuration?.job?.id)

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

    @Deprecated
    String updateProjectJob() {
        logger.info('Starting updateProjectJob task')
        checkRequiredConfig(!configuration?.job?.id || !configuration?.project?.id)

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

    String upgradeProjectJobWithGraphQL() {
        logger.info('Starting upgradeProjectJob task')
        logger.debug('Using config [job={}, jobVersion={}]', configuration.job, configuration.jobVersion)
        String returnData = null
        Request projectsUpdateJobRequest = saagieUtils.getProjectUpdateJobFromDataRequest()
        updateProjectJobWithGraphQLFromRequest(projectsUpdateJobRequest)
        def updatedJobVersion = addJobVersionFromConfiguration()
         Map upgradeStatus = [status: 'success', version: updatedJobVersion]
        return JsonOutput.toJson(upgradeStatus)
    }


    String updateProjectJobWithGraphQLFromParams(Job job, JobVersion jobVersion) {
        logger.info('Starting updateProjectJob task')
        logger.debug('Using config [job={}, jobVersion={}]', job, jobVersion)
        String returnData = null
        Request projectsUpdateJobRequest = saagieUtils.getProjectUpdateJobFromDataRequestFromParams(job)
        returnData = updateProjectJobWithGraphQLFromRequest(projectsUpdateJobRequest)

        addJobVersionFromConfiguration()

        return returnData
    }

    String updateProjectJobFromParams(job, jobVersion) {
        logger.info('Starting updateProjectJob task')
        logger.debug('Using config [job={}, jobVersion={}]', configuration.job, configuration.jobVersion)
        String returnData = null
        Request projectsUpdateJobRequest = saagieUtils.getProjectUpdateJobRequest()
        returnData = updateProjectJobWithGraphQLFromRequest(projectsUpdateJobRequest)

        addJobVersionFromConfiguration()

        return returnData
    }

    //getProjectUpdateJobFromDataRequest
    String updateProjectJobWithGraphQLFormatted() {
        logger.info('Starting updateProjectJobFromData task')

        logger.debug('Using config [job={}, jobVersion={}]', configuration.job, configuration.jobVersion)

        Request projectsUpdateJobRequest = saagieUtils.getProjectUpdateJobFromDataRequest()
        String returnData = updateProjectJobWithGraphQLFromRequest(projectsUpdateJobRequest)

        addJobVersionFromConfiguration()

        return returnData
    }

    String updateProjectJobWithGraphQLFromRequest(Request request) {
        checkRequiredConfig(
            !configuration?.job?.id ||
                (configuration?.job?.isScheduled && !configuration?.job?.cronScheduling) ||
                (configuration?.jobVersion?.exists() && !configuration?.jobVersion?.runtimeVersion)
        )
        String returnData = null
        try {
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
                    returnData = JsonOutput.toJson(updatedJob)
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
        return returnData
    }


    String addJobVersionFromConfiguration() {
        // 2. add jobVersion id there is a jobVersion config
        if (configuration?.jobVersion?.exists()) {
            Request addJobVersionRequest
            if (configuration.jobVersion.packageInfo?.name) {
                addJobVersionRequest = saagieUtils.getAddJobVersionRequestWithGraphQL()
            } else {
                if(configuration.jobVersion.packageInfo?.downloadUrl){
                    configuration.jobVersion.usePreviousArtifact = true
                }
                addJobVersionRequest = saagieUtils.getAddJobVersionRequestWithoutFile()
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
        } else if (configuration?.job?.id) {
            Request lisJobVersion  = saagieUtils.getListVersionForJobRequest(configuration?.job?.id)
            client.newCall(lisJobVersion).execute().withCloseable { listJobVersionsResponse ->
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
                            currentNumber =  it.number
                        }
                    }
                    return currentNumber
                }
            }
        }
    }

    String createProjectPipelineJob() {
        logger.info('Starting createPipeline task')
        checkRequiredConfig(!configuration?.project?.id || !configuration?.pipeline?.name)

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
        checkRequiredConfig(!configuration?.jobinstance?.id)

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
        logger.info('Starting upgradeProjectPipeline task')

        // 1. try to update pipeline infos
        checkRequiredConfig(!configuration?.pipeline?.id)
        def pipelineResult = updatePipelineInfos()

        // 2. try to update pipeline version
        if (configuration?.pipelineVersion) {
            checkRequiredConfig(configuration?.pipelineVersion && configuration?.pipelineVersion?.jobs?.isEmpty())
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
                    String updatedPipelineStatus = parsedResult.data.editPipeline ? 'success' : 'failure'
                    Map updatedPipeline = [status: updatedPipelineStatus]
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
        checkRequiredConfig(!configuration?.pipelineinstance?.id)

        logger.debug('Using config [pipelineInstanceId={}]', configuration.pipelineinstance.id)
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
        checkRequiredConfig(!configuration?.pipeline?.id)

        logger.debug('Using config [pipelineId={}]', configuration.pipeline.id)

        Request projectRunPipelineRequest = saagieUtils.getProjectRunPipelineRequest()
        Map runPipelineData;
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
                    runPipelineData = parsedResult.data
                }
            }

            if(runPipelineData?.runPipeline?.id){
                Request projectGetPipelineInstanceStatus = saagieUtils.getProjectPipelineInstanceStatusRequestWithparam(runPipelineData.runPipeline.id);
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
                        Map updatedPipeline = [runPipeline: [id: runPipelineData.runPipeline.id, status: instancePipelineData.pipelineInstance.status ]]
                        return JsonOutput.toJson(updatedPipeline)
                    }
                }
            } else {
                def message = "Something went wrong when intance pipeline"
                logger.error(message)
                throw new GradleException(message)
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
        checkRequiredConfig(!configuration?.jobinstance?.id)

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
                    String stoppedJobInstanceStatus = parsedResult.data.stopJobInstance ? 'success' : 'failure'
                    Map stoppedJobInstance = [status: stoppedJobInstanceStatus]
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
        checkRequiredConfig(!configuration?.pipeline?.id)

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

    String deleteProject() {
        logger.info('Starting deleteProject task')
        if (configuration?.project?.id == null) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', taskName))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', taskName))
        }

        logger.debug('Using config [projectId={}]', configuration.project.id)

        Request projectDeleteRequest = saagieUtils.archiveProjectRequest()
        try {
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
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in deleteProject')
            throw exception
        }
    }

    String deleteProjectJob() {
        logger.info('Starting deleteProjectJob task')
        checkRequiredConfig(!configuration?.job?.id)

        logger.debug('Using config [jobId={}]', configuration.job.id)

        Request projectArchiveJobRequest = saagieUtils.getProjectArchiveJobRequest()
        try {
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
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in deleteProjectJob')
            throw exception
        }
    }

    String stopPipelineInstance() {
        logger.info('Starting stopPipelineInstance task')
        checkRequiredConfig(!configuration?.pipelineinstance?.id)

        logger.debug('Using config [pipelineInstanceId={}]', configuration.pipelineinstance.id)

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
                    String stoppedPipelineInstanceStatus = parsedResult.data.stopPipelineInstance ? 'success' : 'failure'
                    Map stoppedPipeline = [status: stoppedPipelineInstanceStatus]
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

    String listPlatforms() {
        logger.info('Starting platformList task')

        checkRequiredConfig(
            !configuration.server?.jwt ||
                !configuration.server?.realm
        )

        Request platformListRequest = saagieUtils.getPlatformListRequest()
        try {
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
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in listPlatforms')
            throw exception
        }
    }

    String listAllPipelines() {
        logger.info('Starting projectsListAllPipelines task')
        checkRequiredConfig(!configuration?.project?.id)

        Request platformListRequest = saagieUtils.getListAllPipelinesRequest()
        try {
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
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in listAllPipelines')
            throw exception
        }
    }

    String listTechnologies() {
        logger.info('Starting technologyList task')

        Request technologyListRequest = saagieUtils.getListAllTechnologiesRequest()
        try {
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
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in listTechnologies')
            throw exception
        }
    }

    String listGroups() {
        logger.info('Starting groupList task')

        checkRequiredConfig(
            !configuration.server?.jwt ||
                !configuration.server?.realm || !configuration.server?.environment
        )

        Request groupListRequest = saagieUtils.getGroupListRequest()
        try {
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
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in groupList')
            throw exception
        }
    }

    String createProject() {
        logger.info('Starting projectsCreate task')

        checkRequiredConfig(!configuration?.project?.name)

        Request projectsCreateRequest = saagieUtils.getProjectsCreateRequest()
        try {
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
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in projectsCreate')
            throw exception
        }
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



        return export(exportPipelines, exportJobs, listJobsByNameAndIdFromV1, true)
    }

    String exportArtifacts() {
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

        return export(exportPipelines, exportJobs)

    }


    String export(ExportPipeline[] exportPipelines, ExportJobs[] exportJobs, listJobWithNameAndIdV1 = null, isV1 = false) {

        ExportContainer exportContainer = [configuration]
        boolean bool = false
        def tempJobDirectory = null

        (bool, tempJobDirectory) = getTemporaryFile(configuration.exportArtifacts.temporary_directory, bool)

        FolderGenerator folder = [
            tempJobDirectory.getAbsolutePath(),
            saagieUtils,
            client,
            configuration,
            exportContainer.fileName,
            exportContainer.overwrite
        ]

        try {
            def listJobs = null
            if(isV1) {
                listJobs = listJobWithNameAndIdV1
            } else {
                listJobs =  getProjectJobsByNameAndId()
            }
            def inputDirectoryToZip = tempJobDirectory.getAbsolutePath() + File.separator + exportContainer.fileName
            folder.exportJobList = exportJobs
            folder.exportPipelineList = exportPipelines
            folder.jobList = listJobs
            folder.generateFolderFromParams()
            ZippingFolder zippingFolder = [exportContainer.exportConfigPath, inputDirectoryToZip, bool]
            zippingFolder.generateZip(tempJobDirectory)

            logger.debug("path after: {}, ", exportContainer.exportConfigPath)

            return JsonOutput.toJson([
                status    : "success",
                exportfile: exportContainer.exportConfigPath
            ])

        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in getJobAndJobVersionDetailToExport')
            throw exception
        }
    }

    def getProjectJobsByNameAndId() {
        Request jobsListRequest = saagieUtils.getProjectJobsRequestGetNameAndId()
        client.newCall(jobsListRequest).execute().withCloseable { responseJobList ->
            try {
                handleErrors(responseJobList)
                String responseBodyForJobList = responseJobList.body().string()
                def parsedResultForJobList = slurper.parseText(responseBodyForJobList)

                if (parsedResultForJobList.data?.jobs) {
                    return parsedResultForJobList.data.jobs
                }
            } catch (Exception exception) {
                logger.error('Unknown error in getProjectJobsRequestGetNameAndId')
                throw exception
            }

        }
    }

    static getTemporaryFile(String url, boolean bool) {
        def tempJobDirectory = null

        if(url){
            tempJobDirectory = new File(url)
            bool = tempJobDirectory.exists()
            if(!bool){
                throw new GradleException("Could not find temporary directory, verify again please")
            }
        }

        if(!bool){
            tempJobDirectory = File.createTempDir("artifacts", ".tmp")
            System.out.println("Directory created successfully");
        }

        if (tempJobDirectory.canWrite()) {
            logger.debug("Directory is created path {}", tempJobDirectory.getAbsolutePath())
        } else {
            throw new GradleException("Cannot Write inside temporary directory")
        }

        return [ bool, tempJobDirectory]
    }

    ExportJobs getJobAndJobVersionDetailToExport(String jobId) {

        checkRequiredConfig(!configuration?.project?.id ||
            !configuration?.job?.ids ||
            !configuration?.exportArtifacts?.export_file
        )

        Request getJobDetail = saagieUtils.getJobDetailRequestFromParam(jobId)
        ExportJobs exportJob = new ExportJobs()
        try {
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
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in getJobAndJobVersionDetailToExport')
            throw exception
        }
    }

    ExportJobs getJobAndJobVersionDetailToExportV1(String jobId, listJobsByNameAndIds) {

        checkRequiredConfig(
            !configuration?.job?.ids ||
            !configuration?.exportArtifacts?.export_file
        )

        Request getJobDetailV1 = saagieUtils.getJobDetailRequestFromParamV1 jobId
        ExportJobs exportJob = new ExportJobs()
        exportJob.isV1 = true
        try {
            getV1Client().newCall(getJobDetailV1).execute().withCloseable { response ->
                handleErrors response
                String responseBody = response.body().string()
                def parsedV1job = slurper.parseText responseBody

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
                if(!technologyV2) {
                    throwAndLogError("No technology found from the v1 version to the v2 version")
                }
                def versionV1 = null
                if(parsedV1job?.current?.options?.language_version) {
                    versionV1 = parsedV1job.current.options.language_version
                }

                def resultTechnologiesVersions = TechnologyService.instance.getTechnologyVersions(technologyV2.id)
                def technologyV2Version = null
                if(resultTechnologiesVersions) {
                    technologyV2Version = TechnologyService.instance.getMostRelevantTechnologyVersion(technologyV2.id, versionV1)
                }
                def formatedCronFromSchedule = SaagieUtils.convertScheduleV1ToCron(parsedV1job.schedule)

                def current = null

                if(parsedV1job.current) {
                    current = parsedV1job.current
                } else {
                    ArrayList jobV1Versions = parsedV1job.versions as ArrayList
                    jobV1Versions.Sort()
                    jobV1Versions.Reverse()
                    current = jobV1Versions[0]
                }

                if(!current) {
                    throwAndLogError("Current can't be null")
                }

                exportJob.setJobFromV1ApiResult(
                    parsedV1job,
                    technologyV2,
                    formatedCronFromSchedule
                )

                def technologieLabel = technologyV2Version && technologyV2Version.versionLabel ?
                    technologyV2Version.versionLabel : null

                exportJob.setJobVersionFromV1ApiResult(
                    parsedV1job,
                    technologieLabel,
                    current
                )

                if(current.file) {
                    exportJob.downloadUrl = current.file
                    exportJob.downloadUrlVersion = current.number
                }
            }

            return exportJob
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in getJobAndJobVersionDetailToExport')
            throw exception
        }
    }

    boolean testIfJobV1isValid(parsedV1job) {
        if (!parsedV1job.capsule_code) {
            throwAndLogError("Something went wrong when getting job detail from v1: $responseBody for job id $jobId")
        }

        if([JobV1Type.jupiter].contains(parsedV1job.capsule_code)) {
            throwAndLogError("JUPYTER type not supported by current job task for job id $jobId")
        }

        if([JobV1Type.docker].contains(parsedV1job.capsule_code)) {
            if(parsedV1job.capsule_code != JobV1Category.processing) {
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

        Request getPipelineDetailV1 = saagieUtils.getPipelineRequestFromParamV1(pipelineId)
        def pipeline = configuration.pipeline
        ExportPipeline exportPipeline = new ExportPipeline()
        try {
            getV1Client().newCall(getPipelineDetailV1).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedV1PipelineResult = slurper.parseText(responseBody)
                def pipelineName= parsedV1PipelineResult.name
                    if (pipelineName) {
                        if(parsedV1PipelineResult.jobs  instanceof Collection && parsedV1PipelineResult.jobs.size() > 0) {
                            parsedV1PipelineResult.jobs.each { element ->
                                testIfJobV1isValid(element)
                            }
                        } else {
                            throwAndLogError("Jobs list must be a not empty ArrayList")
                        }

                        listJobsWithNameAndIdFromV1.addAll(parsedV1PipelineResult.jobs.collect { job ->
                            [
                                id: job.id,
                                name: job.name
                            ]
                        })

                        if (pipeline.include_job) {
                            if(!listJobsWithNameAndIdFromV1 instanceof ArrayList){
                                throwAndLogError("listJobsByNameAndIdFromV1 must be a list of type ArrayList")
                            }

                            configuration.job.ids.addAll(listJobsWithNameAndIdFromV1.collect { job -> job.id})

                        }
                        exportPipeline.setPipelineFromV1ApiResult(parsedV1PipelineResult)
                        exportPipeline.setPipelineVersionFromV1ApiResult(
                            parsedV1PipelineResult.jobs.collect{ job ->
                                [id: job.id]
                            },
                            parsedV1PipelineResult.hasProperty("releaseNote") ? parsedV1PipelineResult.releaseNote : null
                        )

                   }
            }
            return exportPipeline

        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in getPipelineAndPipelineVersionDetailToExport')
            throw exception
        }
    }

    ExportPipeline getPipelineAndPipelineVersionDetailToExport(String pipelineId) {

        checkRequiredConfig(!configuration?.project?.id ||
            !configuration?.pipeline?.ids ||
            !configuration?.exportArtifacts?.export_file
        )

        Request getPipelineDetail = saagieUtils.getPipelineRequestFromParam(pipelineId)
        def pipeline = configuration.pipeline
        ExportPipeline exportPipeline = new ExportPipeline()
        try {
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

        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in getPipelineAndPipelineVersionDetailToExport')
            throw exception
        }
    }

    ExportPipeline[] getListPipelineAndPipelineVersionsFromConfig() {
        return getPipelineAndPipelineVersions(this.&getPipelineAndPipelineVersionDetailToExport)
    }
    ExportPipeline[] getListPipelineAndpipelineVersionsFromConfigV1(ArrayList listJobsByNameAndIdFromV1){
        return getPipelineAndPipelineVersions(this.&getPipelineAndPipelineVersionDetailToExportV1, listJobsByNameAndIdFromV1)
    }

    ExportPipeline[] getPipelineAndPipelineVersions(Closure operation, listJobsByNameAndIdFromV1) {
        checkRequiredConfig(
            !configuration?.pipeline?.ids ||
            !configuration?.exportArtifacts?.export_file
        )
        def listPipelineIds = configuration.pipeline.ids.unique { a, b -> a <=> b }
        def arrayPipelines = []
        listPipelineIds.each { pipelineId ->
            def sPipeLineInd = pipelineId as String
            arrayPipelines.add(operation(sPipeLineInd, listJobsByNameAndIdFromV1))
        }
        return arrayPipelines as ExportPipeline[]
    }

    ExportJobs[] getListJobAndJobVersionsFromConfig() {
        return getListJobAndJobVersions(this.&getJobAndJobVersionDetailToExport)
    }

    ExportJobs[] getListJobAndJobVersionsFromConfigV1(ArrayList listJobsByNameAndIdFromV1) {
        return getListJobAndJobVersions(this.&getJobAndJobVersionDetailToExportV1, listJobsByNameAndIdFromV1)
    }

    ExportJobs[] getListJobAndJobVersions(Closure operation, listJobsByNameAndIdFromV1) {
        checkRequiredConfig(
            !configuration?.job?.ids ||
            !configuration?.exportArtifacts?.export_file
        )
        def listJobIdsInt = configuration.job.ids.collect { it as int }
        def listJobIds = listJobIdsInt.unique { a, b -> a <=> b }
        def arrayJobs = []

        listJobIds.each { jobId ->
            arrayJobs.add(operation(jobId as String, listJobsByNameAndIdFromV1))
        }
        return arrayJobs as ExportJobs[]
    }

    String callGetJobDetail() {

        checkRequiredConfig(!configuration?.project?.id ||
            !configuration?.job?.id
        )

        Request getJobDetail = saagieUtils.getJobDetailRequest()
        try {
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
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in projectsCreate')
            throw exception
        }
    }

    String updateProject() {
        logger.info('Starting projectsUpdate task')

        checkRequiredConfig(!configuration?.project?.id)

        Request projectsUpdateRequest = saagieUtils.getProjectsUpdateRequest()
        try {
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
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in projectsUpdate')
            throw exception
        }
    }

    String importJob() {
        logger.info('Starting projectsUpdate task')

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
        boolean bool = false


        (bool, tempFolder) = getTemporaryFile(configuration.importArtifacts.temporary_directory, bool)

        try {
            ZipUtils.unzip(exportedJobFilePath, tempFolder.absolutePath)
        } catch (IOException e) {
            logger.error('An error occurred when unzipping the artifacts export file.', e.message)
        }

        def exportedJobZipNameWithoutExt = exportedJob.name.replaceFirst(~/\.[^\.]+$/, '')
        def exportedArtifactsPathRoot = new File("${tempFolder.absolutePath}/${exportedJobZipNameWithoutExt}")
        def jobsConfigFromExportedZip = SaagieClientUtils.extractJobConfigAndPackageFromExportedJob(exportedArtifactsPathRoot)
        def pipelinesConfigFromExportedZip = SaagieClientUtils.extractPipelineConfigAndPackageFromExportedPipeline(exportedArtifactsPathRoot)
        def response = [
            status  : 'success',
            job     : [],
            pipeline: []
        ]
        def listJobs = null
        def callbackJobToDebug = { newMappedJobData, job, id ->
            configuration.job = newMappedJobData.job
            configuration.jobVersion = newMappedJobData.jobVersion
            configuration.job.id =  null

            listJobs = getJobListByNameAndId()

            if (listJobs) {
                boolean nameExist = false
                def foundNameId = null
                listJobs.each {
                    if (it.name == newMappedJobData.job.name) {
                        nameExist = true
                        foundNameId = it.id
                    }
                }
                // the job do not exists, create it
                if (nameExist) {
                    configuration.job.id = foundNameId
                    addJobVersionFromConfiguration()
                } else {
                    createProjectJobWithOrWithFile()
                }
            } else {
                createProjectJobWithOrWithFile()
            }

            response.job << [
                id  : job.key,
                name: newMappedJobData.job.name
            ]

        }

        def listPipelines = null;
        def callbackPipelinesToDebug = { newMappedPipeline, pipeline, id ->
            configuration.pipeline = newMappedPipeline.pipeline
            configuration.pipelineVersion = newMappedPipeline.pipelineVersion
            configuration.pipeline.id =  null
            listPipelines = getPipelineListByNameAndId()
            if (listPipelines) {
                boolean nameExist = false
                def pipelineFoundId = null;
                listPipelines.each {
                    if (it.name == newMappedPipeline.pipeline.name) {
                        pipelineFoundId = it.id
                        nameExist = true
                    }
                }
                // the pipeline do not exists, create it

                if (nameExist) {
                    configuration.pipeline.id = pipelineFoundId
                    updatePipelineVersion()
                } else {
                    createProjectPipelineJob()
                }
            } else {
                createProjectPipelineJob()
            }

            response.pipeline << [
                id  : pipeline.key,
                name: newMappedPipeline.pipeline.name
            ]
        }

        if (jobsConfigFromExportedZip && jobsConfigFromExportedZip.jobs) {
            ImportJobService.importAndCreateJobs(
                jobsConfigFromExportedZip.jobs,
                configuration,
                callbackJobToDebug
            )
        }


        if (pipelinesConfigFromExportedZip && pipelinesConfigFromExportedZip.pipelines && response.status == 'success') {
            def newJobList = getJobListByNameAndId()
            ImportPipelineService.importAndCreatePipelines(
                pipelinesConfigFromExportedZip.pipelines,
                configuration,
                callbackPipelinesToDebug,
                newJobList
            )
        }

        if(bool){
            SaagieUtils.cleanDirectory(exportedArtifactsPathRoot, logger)
        }else{
            SaagieUtils.cleanDirectory(tempFolder, logger)
        }

        return response
    }


    private getJobListByNameAndId() {
        def listJobs = null
        try {
            Request jobsListRequest = saagieUtils.getProjectJobsRequestGetNameAndId()
            client.newCall(jobsListRequest).execute().withCloseable { responseJobList ->
                handleErrors(responseJobList)
                String responseBodyForJobList = responseJobList.body().string()
                def parsedResultForJobList = slurper.parseText(responseBodyForJobList)
                if (parsedResultForJobList.data?.jobs) {
                    listJobs = parsedResultForJobList.data.jobs
                }
                return listJobs
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in projectsImport')
            throw exception
        }
    }

    private getPipelineListByNameAndId() {
        def listPipelines = null;
        try {
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
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        } catch (Exception exception) {
            logger.error('Unknown error in projectsImport')
            throw exception
        }
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

    static throwAndLogError(message){
        SaagieUtils.throwAndLogError(logger, message)
    }

    def getV1Client() {
         HttpClientBuilder.getHttpClientV1(configuration)
    }

    def isArray(array) {
        return array != null && array.getClass().isArray()
    }
}
