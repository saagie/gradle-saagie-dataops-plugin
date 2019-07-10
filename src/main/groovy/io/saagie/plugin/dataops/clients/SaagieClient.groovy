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
    }

    String getProjects() {
        Request projectsRequest = saagieUtils.getProjectsRequest();
        try {
            client.newCall(projectsRequest).execute().withCloseable { response ->
                if (response.isSuccessful()) {
                    String responseBody = response.body().string()
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
        } catch (Exception ignored) {
            throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_LIST_TASK))
        }
    }

    String getProjectJobs() {
        if (configuration.project == null ||
            configuration.project.id == null ||
            !configuration.project.id instanceof  String
        ) {
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_JOBS_TASK))
        }

        Request projectJobsRequest = saagieUtils.getProjectJobsRequest()
        try {
            client.newCall(projectJobsRequest).execute().withCloseable { response ->
                if (response.isSuccessful()) {
                    String responseBody = response.body().string()
                    def parsedResult = slurper.parseText(responseBody)
                    if (parsedResult.data == null) {
                        throw new StopActionException('Something went wrong when getting project jobs.')
                    } else {
                        List jobs = parsedResult.data.jobs
                        return JsonOutput.toJson(jobs)
                    }
                } else {
                    throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_LIST_JOBS_TASK))
                }
            }
        } catch (Exception ignored) {
            throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_LIST_JOBS_TASK))
        }
    }

    String getProjectTechnologies() {
        if (configuration?.project?.id == null
        ) {
            throw new InvalidUserDataException(
                BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_LIST_TECHNOLOGIES_TASK)
            )
        }

        Request projectTechnologiesRequest = saagieUtils.getProjectTechnologiesRequest()
        try {
            client.newCall(projectTechnologiesRequest).execute().withCloseable { response ->
                if (response.isSuccessful()) {
                    String responseBody = response.body().string()
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
        } catch (Exception ignored) {
            throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_LIST_TECHNOLOGIES_TASK))
        }
    }

    String createProjectJob() {
        if (configuration?.project?.id == null ||
            configuration?.job?.name == null ||
            configuration?.job?.technology == null ||
            configuration?.job?.category == null ||
            configuration?.jobVersion?.runtimeVersion == null ||
            configuration?.jobVersion?.resources == null
        ) {
            throw new InvalidUserDataException(
                BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_CREATE_JOB_TASK)
            )
        }

        if (configuration.jobVersion.packageInfo?.name != null) {
            File scriptToUpload = new File(configuration.jobVersion.packageInfo.name)
            if (!scriptToUpload.exists()) {
                throw new InvalidUserDataException(NO_FILE_MSG.replaceAll('%FILE%', configuration.jobVersion.packageInfo.name))
            }
        }

        Request projectCreateJobRequest = saagieUtils.getProjectCreateJobRequest()
        try {
            client.newCall(projectCreateJobRequest).execute().withCloseable { response ->
                if (response.isSuccessful()) {
                    String responseBody = response.body().string()
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
                                throw new InvalidUserDataException(
                                    BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_CREATE_JOB_TASK)
                                )
                            }
                        }
                    }
                } else {
                    throw new InvalidUserDataException(
                        BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_CREATE_JOB_TASK)
                    )
                }
            }
        } catch (FileNotFoundException ignored) {
            throw new InvalidUserDataException(
                NO_FILE_MSG.replaceAll('%FILE%', configuration.jobVersion.packageInfo.name)
            )
        } catch (Exception ignored) {
            throw new InvalidUserDataException(
                BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_CREATE_JOB_TASK)
            )
        }
    }

    String runProjectJob() {
        if (configuration?.job?.id == null) {
            throw new InvalidUserDataException(
                BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_RUN_JOB_TASK)
            )
        }

        Request runProjectJobRequest = saagieUtils.getRunProjectJobRequest()
        try {
            client.newCall(runProjectJobRequest).execute().withCloseable { response ->
                if (response.isSuccessful()) {
                    String responseBody = response.body().string()
                    def parsedResult = slurper.parseText(responseBody)
                    if (parsedResult.data == null) {
                        throw new StopActionException('Something went wrong when requesting the job to run.')
                    } else {
                        Map runningJob = parsedResult.data.runJob
                        return JsonOutput.toJson(runningJob)
                    }
                } else {
                    throw new InvalidUserDataException(
                        BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_RUN_JOB_TASK)
                    )
                }
            }
        } catch (Exception ignored) {
            throw new InvalidUserDataException(
                BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_RUN_JOB_TASK)
            )
        }
    }

    def updateProjectJob() {
        if (configuration?.project?.id == null ||
            !configuration?.project?.id instanceof String ||
            configuration?.job?.id == null
        ) {
            throw new InvalidUserDataException(BAD_PROJECT_CONFIG.replaceAll('%WIKI%', PROJECT_UPDATE_JOB_TASK))
        }

        Request request = saagieUtils.getProjectUpdateJobRequest()
        try {
            client.newCall(request).execute().withCloseable { response ->
                if (response.isSuccessful()) {
                    def responseBody = response.body().string()
                    def parsedResult = slurper.parseText(responseBody)
                    if (parsedResult.data == null) {
                        throw new StopActionException('Something went wrong when updating project job.')
                    } else {
                        Map updatedJob = parsedResult.data.editJob
                        return JsonOutput.toJson(updatedJob)
                    }
                } else {
                    throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_UPDATE_JOB_TASK))
                }
            }
        } catch (Exception error) {
            throw new InvalidUserDataException(BAD_CONFIG_MSG.replaceAll('%WIKI%', PROJECT_UPDATE_JOB_TASK))
        }
    }
}
