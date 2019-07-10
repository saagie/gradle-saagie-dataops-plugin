package io.saagie.plugin.dataops.utils

import groovy.json.JsonGenerator
import groovy.transform.TypeChecked
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Job
import io.saagie.plugin.dataops.models.JobVersion
import io.saagie.plugin.dataops.models.Project
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody

@TypeChecked
class SaagieUtils {
    DataOpsExtension configuration
    String credentials

    SaagieUtils(DataOpsExtension configuration) {
        this.configuration = configuration
        this.credentials = Credentials.basic(configuration.server.login, configuration.server.password)
    }

    static String gq(String request, String vars = null) {
        def inlinedRequest = request.replaceAll('\\n', '')
        if (vars != null) {
            """{ "query": "$inlinedRequest", "variables": $vars }"""
        } else {
            """{ "query": "$inlinedRequest" }"""
        }
    }

    static final MediaType JSON = MediaType.parse 'application/json; charset=utf-8'

    Request getProjectsRequest() {
        def listProjectsRequest = gq('''
            {
                projects {
                    id
                    name
                    creator
                    description
                    jobsCount
                    status
                }
            }
        ''')

        buildRequestFromQuery listProjectsRequest
    }

    Request getProjectJobsRequest() {
        Project project = configuration.project

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            projectId: project.id
        ])

        def listProjectJobs = gq('''
            query jobs($projectId: UUID!) {
                jobs(projectId: $projectId) {
                    name
                    description
                    countJobInstance
                    versions {
                        number
                    }
                    category
                    technology {
                        id
                        label
                        isAvailable
                    }
                    isScheduled
                    cronScheduling
                    scheduleStatus
                    alerting {
                        emails
                        statusList
                    }
                    isStreaming
                    creationDate
                    migrationStatus
                    migrationProjectId
                    isDeletable
                }
            }
        ''', gqVariables)
        buildRequestFromQuery listProjectJobs
    }

    Request getProjectTechnologiesRequest() {
        Project project = configuration.project

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            projectId: project.id
        ])

        def listProjectTechnologies = gq('''
            query technologiesQuery($projectId: UUID!) {
                technologies(projectId: $projectId) {
                    id
                    label
                    isAvailable
                    icon
                    features {
                        field
                        label
                        isMandatory
                        comment
                        defaultValue
                    }
                }
            }
        ''', gqVariables)
        buildRequestFromQuery listProjectTechnologies
    }

    Request getProjectCreateJobRequest() {
        Job job = configuration.job
        JobVersion jobVersion = configuration.jobVersion

        job.projectId = configuration.project.id
        def file = new File(jobVersion.packageInfo.name)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .excludeFieldsByName('dockerInfos') // TODO: remove this line when `dockerInfos` will be available
            .addConverter(String) { String value, String key -> key == 'technology' ? [id: value] : value }
            .addConverter(JobVersion) { JobVersion value ->
                value.packageInfo.name = file.name
                return value
            }
            .build()

        def gqVariables = jsonGenerator.toJson([
            job: job,
            jobVersion: jobVersion
        ])

        // quick hack needed because the toJson seems to update the converted object, even with a clone
        jobVersion.packageInfo.name = file.absolutePath

        def createProjectJob = gq('''
            mutation createJob($job: JobInput!, $jobVersion: JobVersionInput!) {
                createJob(job: $job, jobVersion: $jobVersion) {
                    id
                }
            }
        ''', gqVariables)

        buildRequestFromQuery createProjectJob
    }

    Request getProjectUpdateJobRequest() {
        Job job = configuration.job

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            job: job,
        ])

        def updateProjectJob = gq('''
            mutation editJobMutation($job: JobEditionInput!) {
                editJob(job: $job) {
                    id
                }
            }
        ''', gqVariables)
        buildRequestFromQuery updateProjectJob
    }

    Request getUploadFileToJobRequest(String jobId, String jobVersion = 1) {
        def file = new File(configuration.jobVersion.packageInfo.name)
        def fileType = MediaType.parse("text/text")

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart('files', file.name, RequestBody.create(fileType, file))
            .build()

        new Request.Builder()
            .url("${configuration.server.url}/api/v1/projects/platform/${configuration.server.environment}/project/${configuration.project.id}/job/$jobId/version/$jobVersion/uploadArtifact")
            .addHeader('Authorization', credentials)
            .post(body)
            .build()
    }

    Request getRunProjectJobRequest() {
        Job job = configuration.job

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            jobId: job.id
        ])

        def runProjectJobRequest = gq('''
            mutation editJobMutation($jobId: UUID!) {
                runJob(jobId: $jobId) {
                    id
                    status
                }
            }
        ''', gqVariables)
        buildRequestFromQuery runProjectJobRequest
    }

    private Request buildRequestFromQuery(String query) {
        RequestBody body = RequestBody.create(JSON, query)
        new Request.Builder()
            .url("${configuration.server.url}/api/v1/projects/platform/${configuration.server.environment}/graphql")
            .addHeader('Authorization', credentials)
            .post(body)
            .build()
    }
}
