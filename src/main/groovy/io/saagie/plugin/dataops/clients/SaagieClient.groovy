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
import org.gradle.api.tasks.StopActionException

import static io.saagie.plugin.dataops.DataOpsModule.*

class SaagieClient {
    static final Logger logger = Logging.getLogger(SaagieClient.class)
    static BAD_CONFIG_MSG = 'Bad config. Make sure you provide rights params: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/%WIKI%'
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
                    def message = "Something went wrong when getting projectList: $responseBody}"
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
        }
    }

    String getProjectJobs() {
        logger.info('Starting getProjectJob task')
        if (configuration.project == null ||
            configuration.project.id == null
        ) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_JOBS_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_JOBS_TASK))
        }

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
                    logger.debug('Successfully requested project jobs')
                    List jobs = parsedResult.data.jobs
                    return JsonOutput.toJson(jobs)
                }
            }
        } catch (InvalidUserDataException invalidUserDataException) {
            throw invalidUserDataException
        } catch (GradleException stopActionException) {
            throw stopActionException
        }
    }

    String getProjectTechnologies() {
        logger.info('Starting getProjectTechnologies task')
        if (configuration?.project?.id == null) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_TECHNOLOGIES_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_TECHNOLOGIES_TASK))
        }

        Request projectTechnologiesRequest = saagieUtils.getProjectTechnologiesRequest()
        try {
            client.newCall(projectTechnologiesRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = 'Something went wrong when getting project technologies.'
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    List technologies = parsedResult.data.technologies
                    return JsonOutput.toJson(technologies)
                }
            }
        } catch (Exception exception) {
            logger.error('Unknow exception thrown when requesting project technologies')
            throw exception
        }
    }

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

        Request projectCreateJobRequest = saagieUtils.getProjectCreateJobRequest()
        try {
            client.newCall(projectCreateJobRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = 'Something went wrong when creating project job.'
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map createdJob = parsedResult.data.createJob
                    String jobId = createdJob.id
                    Request uploadRequest = saagieUtils.getUploadFileToJobRequest(jobId)
                    client.newCall(uploadRequest).execute().withCloseable { uploadResponse ->
                        handleErrors(uploadResponse)
                        return JsonOutput.toJson(createdJob)
                    }
                }
            }
        } catch (Exception exception) {
            logger.error(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_CREATE_JOB_TASK))
            throw exception
        }
    }

    String runProjectJob() {
        logger.info('Starting runProjectJob task')
        if (configuration?.job?.id == null) {
            logger.error(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_RUN_JOB_TASK))
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_RUN_JOB_TASK))
        }

        Request runProjectJobRequest = saagieUtils.getRunProjectJobRequest()
        try {
            client.newCall(runProjectJobRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = 'Something went wrong when requesting the job to run.'
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map runningJob = parsedResult.data.runJob
                    return JsonOutput.toJson(runningJob)
                }
            }
        } catch (Exception exception) {
            logger.error(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_RUN_JOB_TASK))
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

        Request projectUpdateJopRequest = saagieUtils.getProjectUpdateJobRequest()
        try {
            client.newCall(projectUpdateJopRequest).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedResult = slurper.parseText(responseBody)
                if (parsedResult.data == null) {
                    def message = 'Something went wrong when updating project job.'
                    logger.error(message)
                    throw new GradleException(message)
                } else {
                    Map updatedJob = parsedResult.data.editJob

                    // If we provide a jobVersion config, use it to update the job
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
        } catch (Exception exception) {
            logger.error(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_UPDATE_JOB_TASK))
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
        String status = "${response.code()}"
        def message = "Error $status when requesting on ${configuration.server.url}:\n$body"
        logger.error(message)
        throw new GradleException(message)
    }
}
