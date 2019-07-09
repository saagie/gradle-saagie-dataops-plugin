package io.saagie.plugin.dataops.utils

import groovy.json.JsonGenerator
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Job
import io.saagie.plugin.dataops.models.JobVersion
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody

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
        def listProjectJobs = gq("""
            {
                jobs(projectId: "${configuration.project.id}") {
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
        """)
        buildRequestFromQuery listProjectJobs
    }

    Request getProjectTechnologiesRequest() {
        def listProjectTechnologies = gq("""
            {
                technologies(projectId: "${configuration.project.id}") {
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
        """)
        buildRequestFromQuery listProjectTechnologies
    }

    Request getProjectCreateJobRequest() {
        Job job = configuration.job
        JobVersion jobVersion = configuration.jobVersion

        job.projectId = configuration.project.id

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .excludeFieldsByName('dockerInfos') // TODO: remove this line when `dockerInfos` will be available
            .addConverter(String) { String value, String key -> key == 'technology' ? [id: value] : value }
            .build()

        def gqVariables = jsonGenerator.toJson([
            job: job,
            jobVersion: jobVersion
        ])

        def createProjectJob = gq('''
            mutation createJob($job: JobInput!, $jobVersion: JobVersionInput!) {
                createJob(job: $job, jobVersion: $jobVersion) {
                    id
                }
            }
        ''', gqVariables)

        buildRequestFromQuery createProjectJob
    }

    Request getUploadFileToJobRequest(String jobId) {
        println jobId
        def file = new File(configuration.jobVersion.packageInfo.name)
        println file
        println file.name
        def fileType = MediaType.parse("text/text")
        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart('files', file.name, RequestBody.create(fileType, file))
            .build()

        new Request.Builder()
            .url("${configuration.server.url}/api/v1/projects/platform/${configuration.server.environment}/project/${configuration.project.id}/job/$jobId/version/1/uploadArtifact")
            .addHeader('Authorization', credentials)
            .post(body)
            .build()
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
