package io.saagie.plugin.dataops.clients

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.utils.SaagieUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.StopActionException

import static io.saagie.plugin.dataops.DataOpsModule.*

class SaagieClient {
    static BAD_CONFIG_MSG = 'Bad config. Make sure you provide rights params: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/%WIKI%'
    static BAD_PROJECT_CONFIG = 'Missing project configuration or project id: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/%WIKI%'
    static NO_FILE_MSG = "Check that there is a file in '%FILE%'"

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
    }

    def getProjects() {
        Request request = saagieUtils.getProjectsRequest();
        try {
            client.newCall(request).execute().withCloseable { response ->
                if (response.isSuccessful()) {
                    def responseBody = response.body().string()
                    def parsedResult = slurper.parseText(responseBody)
                    if (parsedResult.data == null) {
                        throw new StopActionException('Something went wrong when getting projectList.')
                    } else {
                        List projects = parsedResult.data.projects
                        return JsonOutput.toJson(projects)
                    }
                } else {
                    throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_LIST_TASK))
                }
            }
        } catch (Exception error) {
            throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_LIST_TASK))
        }
    }

    def getProjectJobs() {
        if (configuration.project == null ||
            configuration.project.id == null ||
            !configuration.project.id instanceof  String
        ) {
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_JOBS_TASK))
        }

        Request request = saagieUtils.getProjectJobsRequest()
        try {
            client.newCall(request).execute().withCloseable { response ->
                if (response.isSuccessful()) {
                    def responseBody = response.body().string()
                    def parsedResult = slurper.parseText(responseBody)
                    if (parsedResult.data == null) {
                        throw new StopActionException('Something went wrong when getting project jobs.')
                    } else {
                        List jobs = parsedResult.data.jobs
                        return JsonOutput.toJson(jobs)
                    }
                } else {
                    println response.body().string()
                    throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_LIST_JOBS_TASK))
                }
            }
        } catch (Exception error) {
            error.printStackTrace()
            throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_LIST_JOBS_TASK))
        }
    }

    def getProjectTechnologies() {
        if (configuration.project == null ||
            configuration.project.id == null ||
            !configuration.project.id instanceof  String
        ) {
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_TECHNOLOGIES_TASK))
        }

        Request request = saagieUtils.getProjectTechnologiesRequest()
        try {
            client.newCall(request).execute().withCloseable { response ->
                if (response.isSuccessful()) {
                    def responseBody = response.body().string()
                    def parsedResult = slurper.parseText(responseBody)
                    if (parsedResult.data == null) {
                        throw new StopActionException('Something went wrong when getting project technologies.')
                    } else {
                        List technologies = parsedResult.data.technologies
                        return JsonOutput.toJson(technologies)
                    }
                } else {
                    throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_LIST_TECHNOLOGIES_TASK))
                }
            }
        } catch (Exception error) {
            throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_LIST_TECHNOLOGIES_TASK))
        }
    }

    def createProjectJob() {
        if (configuration?.project?.id == null ||
            configuration?.job?.name == null ||
            configuration?.job?.technology == null ||
            configuration?.job?.category == null ||
            configuration?.jobVersion?.runtimeVersion == null ||
            configuration?.jobVersion?.resources == null
        ) {
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_CREATE_JOB_TASK))
        }

        if (configuration.jobVersion.packageInfo?.name != null) {
            File scriptToUpload = new File(configuration.jobVersion.packageInfo.name)
            if (!scriptToUpload.exists()) {
                throw new InvalidUserDataException(NO_FILE_MSG.replaceAll('%FILE%', configuration.jobVersion.packageInfo.name))
            }
        }

        Request request = saagieUtils.getProjectCreateJobRequest()
        try {
            client.newCall(request).execute().withCloseable { response ->
                if (response.isSuccessful()) {
                    def responseBody = response.body().string()
                    def parsedResult = slurper.parseText(responseBody)
                    if (parsedResult.data == null) {
                        throw new StopActionException('Something went wrong when creating project job.')
                    } else {
                        Map createdJob = parsedResult.data.createJob
                        String jobId = createdJob.id
                        Request uploadRequest = saagieUtils.getUploadFileToJobRequest(jobId)
                        client.newCall(uploadRequest).execute().withCloseable { uploadResponse ->
                            if (uploadResponse.isSuccessful()) {
                                return JsonOutput.toJson(createdJob)
                            } else {
                                throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_CREATE_JOB_TASK))
                            }
                        }
                    }
                } else {
                    throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_CREATE_JOB_TASK))
                }
            }
        } catch (FileNotFoundException error) {
            throw new InvalidUserDataException(NO_FILE_MSG.replaceAll('%FILE%', configuration.jobVersion.packageInfo.name))
        } catch (Exception error) {
            throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_CREATE_JOB_TASK))
        }
    }
}
