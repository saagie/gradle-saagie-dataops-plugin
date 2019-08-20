package io.saagie.plugin.dataops.utils

import groovy.json.JsonGenerator
import groovy.transform.TypeChecked
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
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
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@TypeChecked
class SaagieUtils {
    static final Logger logger = Logging.getLogger(SaagieUtils.class)
    static final MediaType JSON = MediaType.parse 'application/json; charset=utf-8'
    DataOpsExtension configuration

    SaagieUtils(DataOpsExtension configuration) {
        this.configuration = configuration
    }

    static String gq(String request, String vars = null, String operationName = null) {
        def inlinedRequest = request.replaceAll('\\n', '')
        def query = """{ "query": "$inlinedRequest\""""
        if (vars != null) {
            query += """, "variables": $vars"""
        }

        if (operationName) {
            query += """, "operationName": "$operationName\""""
        }

        query += """ }"""

        logger.debug('Generated graphql query:\n{}', query)
        return query
    }

    Request getProjectsRequest() {
        logger.debug('Generating getProjectsRequest')
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
        logger.debug('Generating getProjectJobsRequest [projectId={}]', project.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            projectId: project.id
        ])

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
        logger.debug('generating getProjectTechnologiesRequest [projectId={}]', project.id)

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
        logger.debug('Generating getProjectCreateJobRequest [job={}, jobVersion={}]', job, jobVersion)

        job.projectId = configuration.project.id
        File file = new File(jobVersion.packageInfo.name)
        logger.debug('Using [file={}] for upload', file.absolutePath)

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
        logger.debug('Generating getProjectUpdateJobRequest [job={}]', job)

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

    Request getAddJobVersionRequest() {
        Job job = configuration.job
        JobVersion jobVersion = configuration.jobVersion
        logger.debug('Generating getAddJobVersionRequest for [jobId={}, jobVersion={}]', job.id, jobVersion)

        def file = new File(jobVersion.packageInfo.name)
        logger.debug('Using [file={}] for upload', file.absolutePath)

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
            jobId: job.id,
            jobVersion: jobVersion
        ])

        // quick hack needed because the toJson seems to update the converted object, even with a clone
        jobVersion.packageInfo.name = file.absolutePath

        def updateProjectJob = gq('''
            mutation addJobVersionMutation($jobId: UUID!, $jobVersion: JobVersionInput!) {
                addJobVersion(jobId: $jobId, jobVersion: $jobVersion) {
                    number
                }
            }
        ''', gqVariables)
        buildRequestFromQuery updateProjectJob
    }

    Request getUploadFileToJobRequest(String jobId, String jobVersion = '1') {
        logger.debug('Generating getUploadFileToJobRequest [jobId={}, jobVersion={}]', jobId, jobVersion)
        def file = new File(configuration.jobVersion.packageInfo.name)
        def fileType = MediaType.parse('text/text')
        logger.debug('Using [file={}] for upload', file.absolutePath)

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart('files', file.name, RequestBody.create(fileType, file))
            .build()

        Server server = configuration.server
        if (server.jwt) {
            logger.debug('Building upload file request with jwt auth...')
            def realm = server.realm
            def jwtToken = server.token
            new Request.Builder()
                .url("${configuration.server.url}/projects/api/platform/${configuration.server.environment}/project/${configuration.project.id}/job/$jobId/version/$jobVersion/uploadArtifact")
                .addHeader('Cookie', "SAAGIETOKEN$realm=$jwtToken")
                .post(body)
                .build()
        } else {
            logger.debug('Building upload file request with basic auth...')
            new Request.Builder()
                .url("${configuration.server.url}/api/v1/projects/platform/${configuration.server.environment}/project/${configuration.project.id}/job/$jobId/version/$jobVersion/uploadArtifact")
                .addHeader('Authorization', getCredentials())
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

    Request getCreatePipelineRequest() {
        Project project = configuration.project;
        Pipeline pipeline = configuration.pipeline;
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

        buildRequestFromQuery runProjectJobRequest
    }

    Request getProjectJobInstanceStatusRequest() {
        JobInstance jobInstance = configuration.jobInstance;
        logger.debug('Generating getProjectJobsRequest [projectId={}]', jobInstance.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            id: jobInstance.id
        ])

        def getJobInstanceStatus = gq('''
            query getJobInstanceStatus($jobId: UUID!) {
                jobInstance(id: $id) {
                    status
                }
            }
        ''', gqVariables)
        buildRequestFromQuery getJobInstanceStatus
    }

    Request getProjectPipelineInstanceStatusRequest() {
        PipelineInstance pipelineInstance = configuration.pipelineInstance
        logger.debug('Generating getProjectPipelineInstanceStatusRequest [pipelineInstanceId={}]', pipelineInstance.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            id: pipelineInstance.id
        ])

        def getPipelineInstanceStatus = gq('''
            query getPipelineInstanceStatus($id: UUID!) {
                pipelineInstance(id: $id) {
                    status
                }
            }
        ''', gqVariables)
        buildRequestFromQuery getPipelineInstanceStatus
    }

    Request getProjectUpdatePipelineRequest() {
        Pipeline pipeline = configuration.pipeline;
        logger.debug('Generating getProjectUpdatePipelineRequest [pipelineId={}]', pipeline.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            pipeline: pipeline
        ])

        def getPipelineInstanceStatus = gq('''
            mutation editPipelineMutation($pipeline: PipelineEditionInput!) {
                editPipeline(pipeline: $pipeline) {
                    id
                }
            }
        ''', gqVariables)
        buildRequestFromQuery getPipelineInstanceStatus
    }

    Request getAddPipelineVersionRequest() {
        Pipeline pipeline = configuration.pipeline;
        PipelineVersion pipelineVersion = configuration.pipelineVersion;
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
        buildRequestFromQuery addPipelineVersionRequest
    }

    Request getProjectRunPipelineRequest() {
        Pipeline pipeline = configuration.pipeline;
        logger.debug('Generating getProjectRunPipelineRequest [pipelineId={}]', pipeline.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            pipelineId: pipeline.id
        ])

        def getJobInstanceStatus = gq('''
            mutation runPipelineMutation($pipelineId: UUID!) {
                runPipeline(pipelineId: $pipelineId) {
                    id
                }
            }
        ''', gqVariables)
        buildRequestFromQuery getJobInstanceStatus
    }

    Request getProjectArchiveJobRequest() {
        Job job = configuration.job;
        logger.debug('Generating getProjectArchiveJobRequest [jobId={}]', job.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            jobId: job.id
        ])

        def getJobInstanceStatus = gq('''
            mutation archiveJobMutation($jobId: UUID!) {
                archiveJob(jobId: $jobId)
            }
        ''', gqVariables)
        buildRequestFromQuery getJobInstanceStatus
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
        JobInstance jobInstance = configuration.jobInstance
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
        buildRequestFromQuery runProjectJobRequest
    }

    private Request buildRequestFromQuery(String query) {
        logger.debug('Generating request from query="{}"', query)
        RequestBody body = RequestBody.create(JSON, query)
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
    private def extractProperties(obj) {
        obj.getClass()
            .declaredFields
            .findAll { !it.synthetic }
            .collectEntries { field ->
                [field.name, obj["$field.name"]]
            }
    }
}
