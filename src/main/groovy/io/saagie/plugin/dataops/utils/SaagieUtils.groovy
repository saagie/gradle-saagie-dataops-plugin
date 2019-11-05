package io.saagie.plugin.dataops.utils

import groovy.json.JsonGenerator
import groovy.transform.TypeChecked
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Job
import io.saagie.plugin.dataops.models.JobInstance
import io.saagie.plugin.dataops.models.JobVersion
import io.saagie.plugin.dataops.models.PipelineInstance
import io.saagie.plugin.dataops.models.Pipeline
import io.saagie.plugin.dataops.models.PipelineVersion
import io.saagie.plugin.dataops.models.Project
import io.saagie.plugin.dataops.models.Server
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.tika.Tika
import okhttp3.Response
import okio.Buffer
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@TypeChecked
class SaagieUtils {
    static final Logger logger = Logging.getLogger(SaagieUtils.class)
    static final MediaType JSON = MediaType.parse('application/json; charset=utf-8')
    DataOpsExtension configuration

    SaagieUtils(DataOpsExtension configuration) {
        this.configuration = configuration
    }

    static String gq(String request, String vars = null, String operationName = null) {
        logger.debug("GraphQL Query:")
        logger.debug(request)

        if (vars) {
            logger.debug("GraphQL Variables:")
            logger.debug(vars)
        }

        def inlinedRequest = request.replaceAll('\\n', '')
        def query = """{"query":"$inlinedRequest\""""
        if (vars != null) {
            query += ""","variables":$vars"""
        }

        if (operationName) {
            query += ""","operationName":"$operationName\""""
        }

        query += """}"""

        logger.debug('Generated graphql query:\n{}', query)
        return query
    }

    Request getProjectsRequest() {
        logger.debug('Generating getProjectsRequest')
        def listProjectsRequest = gq('''
            query getProjects {
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

        return buildRequestFromQuery(listProjectsRequest)
    }

    Request getProjectJobsRequest() {
        Project project = configuration.project
        logger.debug('Generating getProjectJobsRequest [projectId={}]', project.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([ projectId: project.id ])

        def listProjectJobs = gq('''
            query jobs($projectId: UUID!) {
                jobs(projectId: $projectId) {
                    id
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
                        loginEmails {
                            login
                            email
                        }
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
        return buildRequestFromQuery(listProjectJobs)
    }

    Request getProjectTechnologiesRequest() {
        Project project = configuration.project
        logger.debug('generating getProjectTechnologiesRequest [projectId={}]', project.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([ projectId: project.id ])

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

        return buildRequestFromQuery(listProjectTechnologies)
    }

    Request getProjectCreateJobRequest() {
        Job job = configuration.job
        JobVersion jobVersion = configuration.jobVersion
        logger.debug('Generating getProjectCreateJobRequest [job={}, jobVersion={}]', job, jobVersion)

        job.projectId = configuration.project.id
        File file = new File(jobVersion.packageInfo.name)
        logger.debug('Using [file={}] for upload', file.absolutePath)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .excludeFieldsByName('dockerInfo') // TODO: remove this line when `dockerInfo` will be available
            .addConverter(JobVersion) { JobVersion value ->
                value.packageInfo.name = file.name
                return value
            }
            .build()

        def gqVariables = jsonGenerator.toJson([
            job: job.toMap(),
            jobVersion: jobVersion.toMap()
        ])

        // quick hack needed because the toJson seems to update the converted object, even with a clone
        jobVersion.packageInfo.name = file.absolutePath

        // Needed because we can't exlude a field from the excludeNull() rule of the JsonGenerator
        def nullFile = '},"file":null}'
        def gqVariablesWithNullFile = "${gqVariables.reverse().drop(2).reverse()}${nullFile}"

        def createProjectJob = gq('''
            mutation createJobMutation($job: JobInput!, $jobVersion: JobVersionInput!, $file: Upload) {
                createJob(job: $job, jobVersion: $jobVersion, file: $file) {
                    id
                    name
                }
            }
        ''', gqVariablesWithNullFile)

        return buildMultipartRequestFromQuery(createProjectJob)
    }

    Request getProjectUpdateJobRequest() {
        Job job = configuration.job
        logger.debug('Generating getProjectUpdateJobRequest [job={}]', job)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            job: job.toMap(),
        ])

        def updateProjectJob = gq('''
            mutation editJobMutation($job: JobEditionInput!) {
                editJob(job: $job) {
                    id
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(updateProjectJob)
    }

    Request getAddJobVersionRequest() {
        Job job = configuration.job
        JobVersion jobVersion = configuration.jobVersion
        logger.debug('Generating getAddJobVersionRequest for [jobId={}, jobVersion={}]', job.id, jobVersion)

        def file = new File(jobVersion.packageInfo.name)
        logger.debug('Using [file={}] for upload', file.absolutePath)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .excludeFieldsByName('dockerInfo') // TODO: remove this line when `dockerInfo` will be available
            .addConverter(JobVersion) { JobVersion value ->
                value.packageInfo.name = file.name
                return value
            }
            .build()

        def gqVariables = jsonGenerator.toJson([
            jobId     : job.id,
            jobVersion: jobVersion.toMap()
        ])

        // quick hack needed because the toJson seems to update the converted object, even with a clone
        jobVersion.packageInfo.name = file.absolutePath

        def nullFile = '},"file":null}'
        def gqVariablesWithNullFile = "${gqVariables.reverse().drop(2).reverse()}${nullFile}"

        def updateProjectJob = gq('''
            mutation addJobVersionMutation($jobId: UUID!, $jobVersion: JobVersionInput!) {
                addJobVersion(jobId: $jobId, jobVersion: $jobVersion) {
                    number
                }
            }
        ''', gqVariablesWithNullFile)

        return buildMultipartRequestFromQuery(updateProjectJob)
    }

    @Deprecated
    Request getUploadFileToJobRequest(String jobId, String jobVersion = '1') {
        logger.debug('Generating getUploadFileToJobRequest [jobId={}, jobVersion={}]', jobId, jobVersion)
        def file = new File(configuration.jobVersion.packageInfo.name)
        Tika tika = new Tika()
        String fileMimeType = tika.detect(file)
        logger.debug('Detected file mime type: ', fileMimeType)
        def fileType = MediaType.parse(fileMimeType)
        logger.debug('Using [file={}] for upload', file.absolutePath)

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart('files', file.name, RequestBody.create(file, fileType))
            .build()

        Server server = configuration.server
        if (server.jwt) {
            logger.debug('Building upload file request with jwt auth...')
            def realm = server.realm
            def jwtToken = server.token
            new Request.Builder()
                .url("${configuration.server.url}/projects/api/platform/${configuration.server.environment}/project/${configuration.project.id}/job/$jobId/version/$jobVersion/uploadArtifact")
                .addHeader('Cookie', "SAAGIETOKEN$realm=$jwtToken")
                .addHeader('Accept', 'application/json')
                .addHeader('Content-Type', 'multipart/form-data')
                .post(body)
                .build()
        } else {
            logger.debug('Building upload file request with basic auth...')
            new Request.Builder()
                .url("${configuration.server.url}/api/v1/projects/platform/${configuration.server.environment}/project/${configuration.project.id}/job/$jobId/version/$jobVersion/uploadArtifact")
                .addHeader('Authorization', getCredentials())
                .addHeader('Accept', 'application/json')
                .addHeader('Content-Type', 'multipart/form-data')
                .post(body)
                .build()
        }
    }

    Request getRunProjectJobRequest() {
        Job job = configuration.job
        logger.debug('Generating getRunProjectJobRequest for job [id={}]', job.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([ jobId: job.id ])

        def runProjectJobRequest = gq('''
            mutation editJobMutation($jobId: UUID!) {
                runJob(jobId: $jobId) {
                    id
                    status
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(runProjectJobRequest)
    }

    Request getCreatePipelineRequest() {
        Project project = configuration.project
        Pipeline pipeline = configuration.pipeline
        PipelineVersion pipelineVersion = configuration.pipelineVersion

        logger.debug('Generating getCreatePipelineRequest for project [projectId={}, pipeline={}, pipelineVersion={}]', project.id, pipeline, pipelineVersion)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def graphqlPipelineVar = [
            *:extractProperties(pipeline),
            projectId: project.id,
            jobsId: pipelineVersion.jobs,
            releaseNote: pipelineVersion.releaseNote
        ]

        def gqVariables = jsonGenerator.toJson([
            pipeline: graphqlPipelineVar
        ])

        def runProjectJobRequest = gq('''
            mutation createPipelineMutation($pipeline: PipelineInput!) {
                createPipeline(pipeline: $pipeline) {
                    id
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(runProjectJobRequest)
    }

    Request getProjectJobInstanceStatusRequest() {
        JobInstance jobInstance = configuration.jobinstance
        logger.debug('Generating getProjectJobsRequest [projectId={}]', jobInstance.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([ jobId: jobInstance.id ])

        def getJobInstanceStatus = gq('''
            query getJobInstanceStatus($jobId: UUID!) {
                jobInstance(id: $jobId) {
                    status
                }
            }
        ''', gqVariables)
        return buildRequestFromQuery(getJobInstanceStatus)
    }

    Request getProjectPipelineInstanceStatusRequest() {
        PipelineInstance pipelineInstance = configuration.pipelineInstance
        logger.debug('Generating getProjectPipelineInstanceStatusRequest [pipelineInstanceId={}]', pipelineInstance.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([ id: pipelineInstance.id ])

        def getPipelineInstanceStatus = gq('''
            query getPipelineInstanceStatus($id: UUID!) {
                pipelineInstance(id: $id) {
                    status
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(getPipelineInstanceStatus)
    }

    Request getProjectUpdatePipelineRequest() {
        Pipeline pipeline = configuration.pipeline
        logger.debug('Generating getProjectUpdatePipelineRequest [pipelineId={}]', pipeline.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([ pipeline: pipeline.toMap() ])

        def editPipeline = gq('''
            mutation editPipelineMutation($pipeline: PipelineEditionInput!) {
                editPipeline(pipeline: $pipeline) {
                    id
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(editPipeline)
    }

    Request getAddPipelineVersionRequest() {
        Pipeline pipeline = configuration.pipeline
        PipelineVersion pipelineVersion = configuration.pipelineVersion
        logger.debug('Generating getAddPipelineVersionRequest [pipelineId={}]', pipeline.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            pipelineId: pipeline.id,
            jobsId: pipelineVersion.jobs,
            releaseNote: pipelineVersion.releaseNote
        ])

        def addPipelineVersionRequest = gq('''
            mutation addPipelineVersionMutation(
                $pipelineId: UUID!,
                $jobsId: [UUID!]!,
                $releaseNote: String,
            ) {
                addPipelineVersion(
                    pipelineId: $pipelineId,
                    jobsId: $jobsId,
                    releaseNote: $releaseNote
                ) {
                    number
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(addPipelineVersionRequest)
    }

    Request getProjectRunPipelineRequest() {
        Pipeline pipeline = configuration.pipeline
        logger.debug('Generating getProjectRunPipelineRequest [pipelineId={}]', pipeline.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([ pipelineId: pipeline.id ])

        def runPipeline = gq('''
            mutation runPipelineMutation($pipelineId: UUID!) {
                runPipeline(pipelineId: $pipelineId) {
                    id
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(runPipeline)
    }

    Request getProjectDeletePipelineRequest() {
        Pipeline pipeline = configuration.pipeline
        logger.debug('Generating getProjectDeletePipelineRequest [pipelineId={}]', pipeline.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([ id: pipeline.id ])

        def deletePipeline = gq('''
            mutation deletePipelineMutation($id: UUID!) {
                deletePipeline(id: $id)
            }
        ''', gqVariables)

        return buildRequestFromQuery(deletePipeline)
    }

    Request getProjectArchiveJobRequest() {
        Job job = configuration.job
        logger.debug('Generating getProjectArchiveJobRequest [jobId={}]', job.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([ jobId: job.id ])

        def getJobInstanceStatus = gq('''
            mutation archiveJobMutation($jobId: UUID!) {
                archiveJob(jobId: $jobId)
            }
        ''', gqVariables)

        return buildRequestFromQuery(getJobInstanceStatus)
    }

    Request getProjectStopPipelineInstanceRequest() {
        PipelineInstance pipelineInstance = configuration.pipelineInstance
        logger.debug('Generating getProjectStopPipelineInstanceRequest [pipelineInstanceId={}]', pipelineInstance.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            pipelineInstanceId: pipelineInstance.id
        ])

        def getJobInstanceStatus = gq('''
            mutation stopPipelineInstanceMutation($pipelineInstanceId: UUID!) {
                stopPipelineInstance(pipelineInstanceId: $pipelineInstanceId) {
                    id
                    status
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(getJobInstanceStatus)
    }

    Request getJwtTokenRequest() {
        logger.debug('Requesting JWT...')
        new Request.Builder()
            .url("${configuration.server.url}/data-fabric/api/auth")
            .addHeader('Authorization', getCredentials())
            .get()
            .build()
    }

    Request getStopJobInstanceRequest() {
        JobInstance jobInstance = configuration.jobinstance
        logger.debug('Generating getStopJobInstanceRequest for job instance [id={}]', jobInstance.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            jobInstanceId: jobInstance.id
        ])

        def runProjectJobRequest = gq('''
            mutation stopJobMutation($jobInstanceId: UUID!) {
                stopJobInstance(jobInstanceId: $jobInstanceId) {
                    id
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(runProjectJobRequest)
    }

    private Request buildMultipartRequestFromQuery(String query) {
        logger.debug('Generating multipart request from query="{}"', query)
        def file = new File(configuration.jobVersion.packageInfo.name)
        def fileName = file.name
        Tika tika = new Tika()
        String mimeType = tika.detect(file)
        logger.debug('Mime type: {}', mimeType)
        def fileType = MediaType.parse(mimeType)
        logger.debug('Using [file={}] for upload', file.absolutePath)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def map = jsonGenerator.toJson([ "0": ["variables.file"] ])
        def fileBody = RequestBody.Companion.newInstance().create(file, fileType)

        RequestBody body = new MultipartBody.Builder("--graphql-multipart-upload-boundary-85763456--")
            .setType(MultipartBody.FORM)
            .addFormDataPart("operations", null, RequestBody.create(query, JSON))
            .addFormDataPart("map", null, RequestBody.create(map, JSON))
            .addFormDataPart('0', fileName, fileBody)
            .build()

        Server server = configuration.server
        if (server.jwt) {
            logger.debug('Generating graphql request with JWT auth...')
            def realm = server.realm
            def jwtToken = server.token
            return new Request.Builder()
                .url("${configuration.server.url}/projects/api/platform/${configuration.server.environment}/graphql")
                .addHeader('Cookie', "SAAGIETOKEN$realm=$jwtToken")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "multipart/form-data")
                .post(body)
                .build()
        } else {
            logger.debug('Generating graphql request with basic auth...')
            Request newRequest = new Request.Builder()
                .url("${configuration.server.url}/api/v1/projects/platform/${configuration.server.environment}/graphql")
                .addHeader('Authorization', getCredentials())
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "multipart/form-data")
                .post(body)
                .build()

            debugRequest(newRequest)

            return newRequest
        }
    }

    private Request buildRequestFromQuery(String query) {
        logger.debug('Generating request from query="{}"', query)
        RequestBody body = RequestBody.create(query, JSON)
        Server server = configuration.server
        if (server.jwt) {
            logger.debug('Generating graphql request with JWT auth...')
            def realm = server.realm
            def jwtToken = server.token
            return new Request.Builder()
                .url("${configuration.server.url}/projects/api/platform/${configuration.server.environment}/graphql")
                .addHeader('Cookie', "SAAGIETOKEN$realm=$jwtToken")
                .post(body)
                .build()
        } else {
            logger.debug('Generating graphql request with basic auth...')
            return new Request.Builder()
                .url("${configuration.server.url}/api/v1/projects/platform/${configuration.server.environment}/graphql")
                .addHeader('Authorization', getCredentials())
                .post(body)
                .build()
        }
    }

    private String getCredentials() {
        Credentials.basic(configuration.server.login, configuration.server.password)
    }

    // From stackoverflow: https://stackoverflow.com/a/36072704/8543172
    private static Map extractProperties(obj) {
        obj.getClass()
            .declaredFields
            .findAll { !it.synthetic }
            .collectEntries { field ->
                [field.name, obj["$field.name"]]
            }
    }

    static debugRequest(Request request) {
        logger.debug("====== Request ======")
        logger.debug("${request.method} ${request.url.url().path}")
        logger.debug("Host: ${request.url.url().host}")
        request.headers().names().each { logger.debug("${it}: ${request.headers().get(it)}") }
        logger.debug("Content-Length: ${request.body().contentLength()}")

        final Buffer buffer = new Buffer()
        request.body().writeTo(buffer)
        logger.debug(buffer.readUtf8())
    }

    static debugResponse(Response response) {
        logger.debug("====== Response ======")
        logger.debug("${response.protocol().toString()} ${response.code} ${response.message}")
        response.headers().names().each { logger.debug("${it}: ${response.headers().get(it)}") }
    }
}
