package io.saagie.plugin.dataops.clients

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.utils.SaagieUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.StopActionException

class SaagieClient {
    static BAD_CONFIG_MSG = 'Bad config. Make sure you provide rights params: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/projectsList'
    static BAD_PROJECT_CONFIG = 'Missing project configuration or project id: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/projectsListJobs'

    DataOpsExtension configuration

    SaagieUtils saagieUtils

    OkHttpClient client = new OkHttpClient()

    JsonSlurper slurper = new JsonSlurper()

    SaagieClient(DataOpsExtension configuration) {
        this.configuration = configuration
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
                    throw new InvalidUserDataException(BAD_CONFIG_MSG)
                }
            }
        } catch (Exception error) {
            throw new InvalidUserDataException(BAD_CONFIG_MSG)
        }
    }

    def getProjectJobs() {
        if (configuration.project == null ||
            configuration.project.id == null ||
            !configuration.project.id instanceof  String
        ) {
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG)
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
                    throw new InvalidUserDataException(BAD_CONFIG_MSG)
                }
            }
        } catch (Exception error) {
            throw new InvalidUserDataException(BAD_CONFIG_MSG)
        }
    }

    def getProjectTechnologies() {
        if (configuration.project == null ||
            configuration.project.id == null ||
            !configuration.project.id instanceof  String
        ) {
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG)
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
                    throw new InvalidUserDataException(BAD_CONFIG_MSG)
                }
            }
        } catch (Exception error) {
            throw new InvalidUserDataException(BAD_CONFIG_MSG)
        }
    }

    def createProjectJob() {
        if (configuration.project == null ||
            configuration.project.id == null ||
            !configuration.project.id instanceof String ||
            configuration.job == null ||
            configuration.jobVersion == null
        ) {
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG)
        }

        Request request = saagieUtils.getProjectCreateJobRequest()
        try {
            client.newCall(request).execute().withCloseable { response ->
                if (response.isSuccessful()) {
                    def responseBody = response.body().string()
                    def parsedResult = slurper.parseText(responseBody)
                    if (parsedResult.data == null) {
                        throw new StopActionException('Something went wrong when getting project technologies.')
                    } else {
                        Map createdProject = parsedResult.data.technologies
                        return JsonOutput.toJson(createdProject)
                    }
                } else {
                    throw new InvalidUserDataException(BAD_CONFIG_MSG)
                }
            }
        } catch (Exception error) {
            throw new InvalidUserDataException(BAD_CONFIG_MSG)
        }
    }
}
