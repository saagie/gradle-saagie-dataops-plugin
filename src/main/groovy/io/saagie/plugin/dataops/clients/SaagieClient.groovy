package io.saagie.plugin.dataops.clients

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Project
import io.saagie.plugin.dataops.tasks.ProjectListTask
import io.saagie.plugin.dataops.utils.SaagieUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.GradleException
import org.gradle.api.GradleScriptException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.StopActionException
import org.gradle.api.tasks.TaskExecutionException

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
                    List projects = parsedResult.data.projects
                    if (parsedResult.hasProperty('error')) {
                        throw new StopActionException('Something went wrong when getting projectList.')
                    } else {
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
        if (!configuration.project || !configuration.project.id) {
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG)
        }

        Request request = saagieUtils.getProjectJobsRequest()
        try {
            client.newCall(request).execute().withCloseable { response ->
                if (response.isSuccessful()) {
                    def responseBody = response.body().string()
                    def parsedResult = slurper.parseText(responseBody)
                    List jobs = parsedResult.data.jobs
                    if (parsedResult.hasProperty('error')) {
                        throw new StopActionException('Something went wrong when getting project jobs.')
                    } else {
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
}
