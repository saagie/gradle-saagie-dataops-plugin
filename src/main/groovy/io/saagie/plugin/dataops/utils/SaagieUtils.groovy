package io.saagie.plugin.dataops.utils

import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.App
import io.saagie.plugin.dataops.models.AppMapper
import io.saagie.plugin.dataops.models.AppVersionDTO
import io.saagie.plugin.dataops.models.Job
import io.saagie.plugin.dataops.models.JobInstance
import io.saagie.plugin.dataops.models.JobMapper
import io.saagie.plugin.dataops.models.JobVersion
import io.saagie.plugin.dataops.models.PipelineInstance
import io.saagie.plugin.dataops.models.Pipeline
import io.saagie.plugin.dataops.models.PipelineVersion
import io.saagie.plugin.dataops.models.Project
import io.saagie.plugin.dataops.models.Server
import io.saagie.plugin.dataops.tasks.projects.enums.UnitTime
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import org.apache.tika.Tika
import okhttp3.Response
import okio.Buffer
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import groovy.json.JsonOutput
import org.threeten.extra.PeriodDuration

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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

    Request getGlobalEnvironmentVariables() {
        logger.debug('Generating globalEnvironmentVariablesQuery');

        def getAllGlobalVariablesQuery = gq(''' query globalEnvironmentVariablesQuery { globalEnvironmentVariables { id name scope value description isPassword } } ''')
        return buildRequestFromQuery(getAllGlobalVariablesQuery)
    }

    Request getProjectEnvironmentVariables(String projectId) {

        logger.debug('Generating getProjectEnvironmentVariables [projectId={}]', projectId)

        def jsonGenerator = new JsonGenerator.Options()
            .build()

        def gqVariables = jsonGenerator.toJson([projectId: projectId])

        def getAllProjectVariablesQuery = gq('''
            query environmentVariablesQuery($projectId: UUID!) {  projectEnvironmentVariables(projectId: $projectId) {    id    scope    name    value    description    isPassword    overriddenValues {      id      scope      value      description      isPassword     }   }}
        ''', gqVariables)
        return buildRequestFromQuery(getAllProjectVariablesQuery)
    }


    Request getAllProjectJobsRequest() {
        getProjectJobsRequestBuild('''
            query jobs($projectId: UUID!) {
                jobs(projectId: $projectId) {
                    id
                    name
                    description
                    countJobInstance
                    versions {
                        number
                        releaseNote
                        runtimeVersion
                        packageInfo {
                            downloadUrl
                        }
                        dockerInfo {
                            image
                            dockerCredentialsId
                        }
                        extraTechnology {
                           language
                           version
                        }
                        commandLine
                        isCurrent
                        isMajor
                        creator
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
        ''')
    }

    Request getProjectJobsGetNameAndIdRequest() {
        getProjectJobsRequestBuild('''
            query jobs($projectId: UUID!) {
                jobs(projectId: $projectId) {
                    id
                    name
                }
            }
        ''')
    }

    Request getProjectPipelinesRequestGetNameAndId() {
        Project project = configuration.project
        logger.debug('Generating getProjectJobsRequest [projectId={}]', project.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([projectId: project.id])

        def listProjectJobs = gq('''
            query pipelines($projectId: UUID!) {
                pipelines(projectId: $projectId) {
                    id
                    name
                }
            }
        ''', gqVariables)
        return buildRequestFromQuery(listProjectJobs)
    }

    Request getProjectGraphPipelinesRequestGetNameAndId() {
        Project project = configuration.project
        logger.debug('Generating getProjectGraphPipelinesRequestGetNameAndId [projectId={}]', project.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([projectId: project.id])

        def listProjectJobs = gq('''
            query getGraphPipelinesByProject($projectId: UUID!) {
                project(id: $projectId) {
                    pipelines {
                        id
                        name
                    }
                }
            }
        ''', gqVariables)
        return buildRequestFromQuery(listProjectJobs)
    }

    static boolean isCollectionOrArray(object) {
        [Collection, Object[]].any { it.isAssignableFrom(object.getClass()) }
    }

    Request getListTechnologyVersionsRequest(String technologyId) {
        logger.debug('Generating getListTechnologyVersionsRequest [technology={}]', technologyId)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([technologyId: technologyId])

        def listTechnologyVersions = gq('''
            query TechnologiesVersions($technologyId: UUID!) {
            technologiesVersions(technologyId: $technologyId) {
              versionLabel
              technologyLabel
              secondaryTechnologies {
                   label     isAvailable      versions
              }
            }
          }
        ''', gqVariables)
        return buildRequestFromQuery(listTechnologyVersions)
    }

    Request getProjectJobsRequestBuild(String query) {
        Project project = configuration.project
        logger.debug('Generating getProjectJobsRequest [projectId={}]', project.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([projectId: project.id])

        def listProjectJobs = gq(query, gqVariables)
        return buildRequestFromQuery(listProjectJobs)
    }

    Request archiveProjectRequest() {
        Project project = configuration.project
        logger.debug('Generating archiveProjectRequest [ProjectId={}]', project.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([projectId: project.id])

        def getProjectInstanceStatus = gq('''
            mutation archiveProjectMutation($projectId: UUID!) {
                archiveProject(projectId: $projectId)
            }
        ''', gqVariables)

        return buildRequestFromQuery(getProjectInstanceStatus)
    }

    Request saveProjectEnvironmentVariable(environmentVariable) {
        Project project = configuration.project
        logger.debug('Generating saveEnvironmentVariable [ProjectId={}]', project.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            entityId: project.id,
            envVar  : environmentVariable
        ])

        def getProjectInstanceStatus = gq('''
            mutation saveGlobalEnvVarMutation($entityId: UUID, $envVar: EnvironmentVariableInput!) {  saveEnvironmentVariable(entityId: $entityId, environmentVariable: $envVar) {    id   name   }}
        ''', gqVariables)

        return buildRequestFromQuery(getProjectInstanceStatus)
    }

    Request saveGlobalEnvironmentVariable(environmentVariable) {
        Project project = configuration.project
        logger.debug('Generating saveEnvironmentVariable [ProjectId={}]', project.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            envVar: environmentVariable
        ])

        def getProjectInstanceStatus = gq('''
            mutation saveGlobalEnvVarMutation($envVar: EnvironmentVariableInput!) { saveEnvironmentVariable(environmentVariable: $envVar) {    id name    }}
        ''', gqVariables)

        return buildRequestFromQuery(getProjectInstanceStatus)
    }

    Request getGlobalVariableByNameAndIdAndScope() {
        logger.debug('Generating getGlobalVariableByNameAndId')

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()
        def getGlobalVariablesByNamesAndId = gq(''' query globalEnvironmentVariablesQuery { globalEnvironmentVariables { id    name  scope } } ''')
        return buildRequestFromQuery(getGlobalVariablesByNamesAndId)
    }

    Request getProjectVariableByNameAndIdAndScope() {
        Project project = configuration.project
        logger.debug('Generating getProjectVariableByNameAndId [ProjectId={}]', project.id)
        def jsonGenerator = getJsonGenerator()

        def gqVariables = jsonGenerator.toJson([projectId: project.id])

        def getGlobalVariablesByNamesAndId = gq(''' query environmentVariablesQuery($projectId: UUID!) { projectEnvironmentVariables(projectId: $projectId) { id  name scope } } ''', gqVariables)
        return buildRequestFromQuery(getGlobalVariablesByNamesAndId)
    }

    static def getJsonGenerator() {
        return new JsonGenerator.Options()
            .excludeNulls()
            .build()
    }

    Request getProjectTechnologiesRequest() {
        Project project = configuration.project
        logger.debug('generating getProjectTechnologiesRequest [projectId={}]', project.id)

        def jsonGenerator = getJsonGenerator()

        def gqVariables = jsonGenerator.toJson([projectId: project.id])

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

//    ================= App request =====================

    Request getAppDetailRequest(String appId) {
        logger.debug('Generating App detail with id', appId)

        def jsonGenerator = new JsonGenerator.Options()
            .build()

        def gqVariables = jsonGenerator.toJson([id: appId])

        def getAppDetailQuery = gq('''
            query labWebApp($id: UUID!) { labWebApp(id: $id) {    id    name    description    creationDate    isDeletable   category   isScheduled   isStreaming   storageSizeInMB    instances(limit: 1, checkInPipelineInstance: false) {      id     status     statusDetails     startTime      endTime      version {        number              }          }    versions {     number      creator      creationDate      number      isCurrent      releaseNote  resources{cpu memory disk}    dockerInfo {        image        dockerCredentialsId             }      exposedPorts {        name        port        isAuthenticationRequired       isRewriteUrl        basePathVariableName              }      storagePaths         }    alerting {      emails      statusList      loginEmails {        login        email             }          }    technology {     id  label         }      }}
        ''', gqVariables)
        return buildRequestFromQuery(getAppDetailQuery)
    }

    Request getAppListByProjectIdRequest() {
        Project project = configuration.project
        logger.debug('Generating app list for project with id', project.id)

        def jsonGenerator = new JsonGenerator.Options()
            .build()

        def gqVariables = jsonGenerator.toJson([projectId: project.id])

        def getAppsListQuery = gq('''
            query labWebAppsQuery($projectId: UUID!) {  labWebApps(projectId: $projectId) {  id   name  }}
        ''', gqVariables)
        return buildRequestFromQuery(getAppsListQuery)
    }

    Request getAppTechnologiesList() {
        logger.debug('Generating technologies for application')

        def jsonGenerator = new JsonGenerator.Options()
            .build()

        def gqVariables = jsonGenerator.toJson([:])

        def getAppTechnologiesListQuery = gq('''
            query repositoriesQuery {  repositories {    id    name    technologies {      ... on AppTechnology {        id        label        description        icon        backgroundColor        available        customFlags              }          }      }}
        ''', gqVariables)
        return buildRequestFromQuery(getAppTechnologiesListQuery, true, true)
    }

    Request updateAppVersion(String appId, AppVersionDTO appVersionDTO) {
        logger.debug('Updating application version for application with id', appId)

        def jsonGenerator = new JsonGenerator.Options()
            .build()

        def gqVariables = jsonGenerator.toJson([
            appId     : appId,
            appVersion: appVersionDTO.toMap()
        ])

        def getUpdateAppVersionQuery = gq('''
            mutation addAppVersionMutation($appId: UUID!, $appVersion: JobVersionInput!) {  addJobVersion(jobId: $appId, jobVersion: $appVersion) {    number      }}
        ''', gqVariables)
        return buildRequestFromQuery(getUpdateAppVersionQuery)
    }

    Request getProjectCreateAppRequest(App app, AppVersionDTO appVersionDTO) {
        Map mapedAppAndAppVersion = AppMapper.mapAppAndAppVersionWithoutMail(app, appVersionDTO, configuration.project.id)
        logger.debug('Generating getProjectCreateAppRequest [app={}, appVersion={}]', app, appVersionDTO)
        return createJobModel(mapedAppAndAppVersion);
    }
//    ========================================

    Request getPipelineRequestFromParamV1(String pipelineId) {
        Server server = configuration.server
        logger.debug('Generating request in order to get pipeline detail from V1')

        Request newRequest = new Request.Builder()
            .url("${server.url}/manager/api/v1/platform/${server.environment}/workflow/${pipelineId}")
            .get()
            .build()

        debugRequest(newRequest)
        return newRequest
    }

    static ArrayList getDifferenceOfTwoArrays(ArrayList collection1, ArrayList collection2) {
        return ((collection1 - collection2) + (collection2 - collection1))
    }

    Request getPipelineInstancesRequestFromParamV1(String pipelineId) {
        Server server = configuration.server
        logger.debug('Generating request in order to get pipeline detail from V1')

        HttpUrl.Builder httpBuilder = HttpUrl.parse("${server.url}/manager/api/v1/platform/${server.environment}/workflow/${pipelineId}/instance").newBuilder();
        httpBuilder.addQueryParameter("page", "1");
        httpBuilder.addQueryParameter("size", "10");

        Request newRequest = new Request.Builder()
            .url(httpBuilder.build())
            .build()

        debugRequest(newRequest)
        return newRequest
    }

    Request getPipelineInstanceDetailRequestFromParamV1(String pipelineId, String instanceId) {
        Server server = configuration.server
        logger.debug('Generating request in order to get pipeline detail from V1')

        Request newRequest = new Request.Builder()
            .url("${server.url}/manager/api/v1/platform/${server.environment}/workflow/${pipelineId}/instance/${instanceId}")
            .get()
            .build()

        debugRequest(newRequest)
        return newRequest
    }

    Request getPipelineRequestFromParam(pipelineId) {
        Project project = configuration.project
        logger.debug('generating getPipelineRequest [pipelineId={}]', project.id)

        def jsonGenerator = getJsonGenerator()

        def gqVariables = jsonGenerator.toJson([pipelineId: pipelineId])

        def pipelineResult = gq('''
            query pipeline ($pipelineId: UUID!) {
                pipeline(id: $pipelineId) {
                    id
                    name
                    description
                    versions {
                        number
                        creationDate
                        releaseNote
                        jobs {
                          id
                        }
                        isCurrent
                        isMajor
                        creator
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

                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(pipelineResult)
    }

    Request getGraphPipelineRequestFromParam(pipelineId) {
        Project project = configuration.project
        logger.debug('generating getGraphPipelineRequest [pipelineId={}]', project.id)

        def jsonGenerator = getJsonGenerator()

        def gqVariables = jsonGenerator.toJson([pipelineId: pipelineId])

        def pipelineResult = gq('''
            query graphPipeline ($pipelineId: UUID!) {
                graphPipeline(id: $pipelineId) {
                    id
                    name
                    description
                    isScheduled
                    cronScheduling
                    alerting {
                        loginEmails {
                            login
                            email
                        }
                        emails
                        statusList
                    }
                    versions {
                        number
                        isCurrent
                        isMajor
                        releaseNote
                        creationDate
                        creator
                        graph {
                            jobNodes {
                                id
                                job {
                                    id
                                }
                                position {
                                    x
                                    y
                                }
                                nextNodes
                            }
                            conditionNodes {
                                id
                                position {
                                    x
                                    y
                                }
                                nextNodesSuccess
                                nextNodesFailure
                            }
                        }
                    }
                }
              }
        ''', gqVariables)

        return buildRequestFromQuery(pipelineResult)
    }


    @Deprecated
    Request getProjectCreateJobRequest(Job job, JobVersion jobVersion) {
        Map mapedJobAndJobVersion = JobMapper.mapJobAndJobVersionWithoutMail(job, jobVersion, configuration.project.id)
        logger.debug('Generating getProjectCreateJobRequest [job={}, jobVersion={}]', job, jobVersion)

        return createJobModel(mapedJobAndJobVersion);
    }

    Request createJobModel(Map jobMapper) {
        logger.debug('Generating createJobModel [jobMaper={}]', jobMapper)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .excludeFieldsByName('usePreviousArtifact')
            .build()

        def gqVariables = jsonGenerator.toJson([
            job       : jobMapper?.job,
            jobVersion: jobMapper?.jobVersion
        ]);

        def createProjectJob = gq('''
            mutation createJobMutation($job: JobInput!, $jobVersion: JobVersionInput!) {
                createJob(job: $job, jobVersion: $jobVersion) {
                    id
                    name
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(createProjectJob)
    }

    Request getProjectCreateJobRequestWithGraphQL(Job job, JobVersion jobVersion) {
        Map mapedJobAndJobVersion = JobMapper.mapJobAndJobVersionWithoutMail(job, jobVersion, configuration.project.id)
        logger.debug('Generating getProjectCreateJobRequest  [job={}, jobVersion={}]', job, jobVersion)

        File file = new File(jobVersion.packageInfo.name)
        logger.debug('Using [file={}] for upload', file.absolutePath)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .excludeFieldsByName('packageInfo')
            .addConverter(JobVersion) { JobVersion value ->
                value.packageInfo.name = file.name
                return value
            }
            .build()

        def gqVariables = jsonGenerator.toJson([
            job       : mapedJobAndJobVersion?.job,
            jobVersion: mapedJobAndJobVersion?.jobVersion
        ])

        // quick hack needed because the toJson seems to update the converted object, even with a clone
//        jobVersion.packageInfo.name = file.absolutePath

        // Needed because we can't exlude a field from the excludeNull() rule of the JsonGenerator
        def nullFile = '},"file":null}'
        def gqVariablesWithNullFile = "${gqVariables.reverse().drop(2).reverse()}${nullFile}"

        def createProjectJob = gq(''' mutation createJobMutation($job: JobInput!, $jobVersion: JobVersionInput!, $file: Upload) { createJob(job: $job, jobVersion: $jobVersion, file: $file) { id name } } ''', gqVariablesWithNullFile)

        return buildMultipartRequestFromQuery(createProjectJob, file, job, jobVersion)
    }

    Request getProjectUpdateJobRequest() {
        Job job = configuration.job
        Map mappedJob = JobMapper.mapJobWithoutMail(job, configuration.project.id)
        logger.debug('Generating getProjectUpdateJobRequest [job={}]', mappedJob)
        def jsonGenerator = getJsonGenerator()
        def gqVariables = jsonGenerator.toJson([
            job: mappedJob,
        ])
        getProjectUpdateJobRequestFormat(gqVariables)
    }

    Request getProjectUpdateJobFromDataRequest() {
        Job job = configuration.job
        Map mappedJob = JobMapper.mapJobWithoutMail(job, configuration.project.id)
        getProjectUpdateJobFromDataRequestFromParams(mappedJob.job)
    }

    Request getProjectUpdateJobFromDataRequestFromParams(job) {
        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()
        Map formattedJob = getFormatForUpdateJob(job)
        def gqVariables = jsonGenerator.toJson([
            job: formattedJob,
        ])
        getProjectUpdateJobRequestFormat(gqVariables)
    }

    Request getProjectUpdateJobRequestFormat(String gqVariables) {
        def updateProjectJob = gq(''' mutation editJobMutation($job: JobEditionInput!) { editJob(job: $job) { id } } ''', gqVariables)


        return buildRequestFromQuery(updateProjectJob)
    }

    @Deprecated
    Request getAddJobVersionRequest() {
        Job job = configuration.job
        JobVersion jobVersion = configuration.jobVersion
        logger.debug('Generating getAddJobVersionRequest for [jobId={}, jobVersion={}]', job.id, jobVersion)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            jobId     : job.id,
            jobVersion: jobVersion.toMap()
        ])

        def updateProjectJob = gq(''' mutation addJobVersionMutation($jobId: UUID!, $jobVersion: JobVersionInput!) { addJobVersion(jobId: $jobId, jobVersion: $jobVersion) { number } } ''', gqVariables)

        return buildRequestFromQuery(updateProjectJob)
    }

    static Map getFormatForUpdateJob(Map data) {
        Set<String> set = new HashSet<>()
        set.add("projectId")
        set.add("category")
        set.add("technology")
        data.keySet().removeAll(set)
        return data
    }

    Request getAddJobVersionRequestWithGraphQL(Job job, JobVersion jobVersion) {
        logger.debug('Generating getAddJobVersionRequest for [jobId={}, jobVersion={}]', job.id, jobVersion)

        def file = new File(jobVersion.packageInfo.name)
        logger.debug('Using [file={}] for upload', file.absolutePath)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .excludeFieldsByName('packageInfo')
            .build()

        def gqVariables = jsonGenerator.toJson([
            jobId     : job.id,
            jobVersion: jobVersion.toMap()
        ])

        // quick hack needed because the toJson seems to update the converted object, even with a clone
        jobVersion.packageInfo.name = file.absolutePath

        def nullFile = '},"file":null}'
        def gqVariablesWithNullFile = "${gqVariables.reverse().drop(2).reverse()}${nullFile}"

        def updateProjectJob = gq(''' mutation addJobVersionMutation($jobId: UUID!, $jobVersion: JobVersionInput!, $file: Upload) { addJobVersion(jobId: $jobId, jobVersion: $jobVersion, file: $file) { number } } ''', gqVariablesWithNullFile)

        return buildMultipartRequestFromQuery(updateProjectJob, file, job, jobVersion)
    }

    Request getAddJobVersionRequestWithoutFile(Job job, JobVersion jobVersion) {
        logger.debug('Generating getAddJobVersionRequest for [jobId={}, jobVersion={}]', job.id, jobVersion)

        def jsonGeneratorParams = new JsonGenerator.Options()
            .excludeNulls()
            .excludeFieldsByName('packageInfo')
        if (!jobVersion.packageInfo?.downloadUrl) {
            jsonGeneratorParams = jsonGeneratorParams.excludeFieldsByName('usePreviousArtifact')
        }
        def jsonGenerator = jsonGeneratorParams.build()
        def gqVariables = jsonGenerator.toJson([
            jobId     : job.id,
            jobVersion: jobVersion.toMap()
        ])

        def updateProjectJob = gq(''' mutation addJobVersionMutation($jobId: UUID!, $jobVersion: JobVersionInput!) { addJobVersion(jobId: $jobId, jobVersion: $jobVersion) { number } } ''', gqVariables)

        return buildRequestFromQuery(updateProjectJob)
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

        def gqVariables = jsonGenerator.toJson([jobId: job.id])

        def runProjectJobRequest = gq(''' mutation editJobMutation($jobId: UUID!) { runJob(jobId: $jobId) { id status } } ''', gqVariables)

        return buildRequestFromQuery(runProjectJobRequest)
    }

    static cleanDirectory(File temp, Logger logger) {
        try {
            temp.deleteDir()
        } catch (Exception exception) {
            logger.warn('The directory couldn\'t be cleaned')
            logger.warn(exception.message)
        }
    }

    Request getCreatePipelineRequest(Pipeline pipeline, PipelineVersion pipelineVersion) {
        Project project = configuration.project

        logger.debug('Generating getCreatePipelineRequest for project [projectId={}, pipeline={}, pipelineVersion={}]', project.id, pipeline, pipelineVersion)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def graphqlPipelineVar = [
            *          : pipeline.toMap(),
            projectId  : project.id,
            jobsId     : pipelineVersion.jobs,
            releaseNote: pipelineVersion.releaseNote
        ]

        def gqVariables = jsonGenerator.toJson([
            pipeline: graphqlPipelineVar
        ])

        def runProjectJobRequest = gq(''' mutation createPipelineMutation($pipeline: PipelineInput!) { createPipeline(pipeline: $pipeline) { id } } ''', gqVariables)

        return buildRequestFromQuery(runProjectJobRequest)
    }

    Request getCreateGraphPipelineRequest(Pipeline pipeline, PipelineVersion graphPipelineVersion, boolean isImportContext) {
        Project project = configuration.project

        logger.debug('Generating getCreateGraphPipelineRequest for project [projectId={}, pipeline={}, graphPipelineVersion={}]', project.id, pipeline, graphPipelineVersion)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def graphqlGraphPipelineVar = [
            *          : pipeline.toMap(),
            projectId  : project.id,
            graph     : isImportContext ? graphPipelineVersion.graph: graphPipelineVersion.graph.toMap(),
            releaseNote: graphPipelineVersion.releaseNote
        ]
        graphqlGraphPipelineVar.remove('id')

        def gqVariables = jsonGenerator.toJson([
            pipeline: graphqlGraphPipelineVar
        ])
        def runProjectJobRequest = gq(''' mutation createGraphPipelineMutation($pipeline: GraphPipelineInput!) { createGraphPipeline(pipeline: $pipeline) { id } } ''', gqVariables)

        return buildRequestFromQuery(runProjectJobRequest)
    }

    Request getProjectJobInstanceStatusRequest() {
        JobInstance jobInstance = configuration.jobinstance
        logger.debug('Generating getProjectJobsRequest [projectId={}]', jobInstance.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([jobId: jobInstance.id])

        def getJobInstanceStatus = gq(''' query getJobInstanceStatus($jobId: UUID!) { jobInstance(id: $jobId) { status } } ''', gqVariables)
        return buildRequestFromQuery(getJobInstanceStatus)
    }

    Request getProjectPipelineInstanceStatusRequest() {
        PipelineInstance pipelineinstance = configuration.pipelineinstance
        logger.debug('Generating getProjectPipelineInstanceStatusRequest [pipelineInstanceId={}]', pipelineinstance.id)


        return getProjectPipelineInstanceStatusRequestWithparam(pipelineinstance.id)
    }

    Request getProjectPipelineInstanceStatusRequestWithparam(String id) {
        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([id: id])

        def getPipelineInstanceStatus = gq(''' query getPipelineInstanceStatus($id: UUID!) { pipelineInstance(id: $id) { status } } ''', gqVariables)

        return buildRequestFromQuery(getPipelineInstanceStatus)
    }

    Request getProjectUpdatePipelineRequest() {
        Pipeline pipeline = configuration.pipeline
        logger.debug('Generating getProjectUpdatePipelineRequest [pipelineId={}]', pipeline.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([pipeline: pipeline.toMap()])

        def editPipeline = gq(''' mutation editPipelineMutation($pipeline: PipelineEditionInput!) { editPipeline(pipeline: $pipeline) { id } } ''', gqVariables)

        return buildRequestFromQuery(editPipeline)
    }

    Request getAddPipelineVersionRequest(Pipeline pipeline, PipelineVersion pipelineVersion) {
        return getAddPipelineVersionRequestFromParams(pipeline, pipelineVersion)

    }

    Request getAddPipelineVersionRequestFromParams(Pipeline pipeline, PipelineVersion pipelineVersion) {
        logger.debug('Generating getAddPipelineVersionRequest [pipelineId={}]', pipeline.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            pipelineId : pipeline.id,
            jobsId     : pipelineVersion.jobs,
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

    Request getAddGraphPipelineVersionRequest(Pipeline pipeline, PipelineVersion graphPipelineVersion, boolean isImportContext) {
        logger.debug('Generating getAddGraphPipelineVersionRequest [pipelineId={}]', pipeline.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            pipelineId : pipeline.id,
            graph     : isImportContext ? graphPipelineVersion.graph: graphPipelineVersion.graph.toMap(),
            releaseNote: graphPipelineVersion.releaseNote
        ])

        def addPipelineVersionRequest = gq('''
            mutation addGraphPipelineVersionMutation(
                $pipelineId: UUID!,
                $graph: PipelineGraphInput!,
                $releaseNote: String,
            ) {
                addGraphPipelineVersion(
                    pipelineId: $pipelineId,
                    graph: $graph,
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

        def gqVariables = jsonGenerator.toJson([pipelineId: pipeline.id])

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

        def gqVariables = jsonGenerator.toJson([id: pipeline.id])

        def deletePipeline = gq('''
            mutation deletePipelineMutation($id: UUID!) {
                deletePipeline(id: $id)
            }
        ''', gqVariables)

        return buildRequestFromQuery(deletePipeline)
    }

    Request getProjectArchiveJobRequest() {
        Job job = configuration.job
        logger.debug('Generating getProjectDeleteJobRequest [jobId={}]', job.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([jobId: job.id])

        def getJobInstanceStatus = gq('''
            mutation archiveJobMutation($jobId: UUID!) {
                archiveJob(jobId: $jobId)
            }
        ''', gqVariables)

        return buildRequestFromQuery(getJobInstanceStatus)
    }

    Request getProjectStopPipelineInstanceRequest() {
        PipelineInstance pipelineinstance = configuration.pipelineinstance
        logger.debug('Generating getProjectStopPipelineInstanceRequest [pipelineinstanceId={}]', pipelineinstance.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            pipelineInstanceId: pipelineinstance.id
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

        Request newRequest = new Request.Builder()
            .url("${configuration.server.url}/authentication/api/open/authenticate")
            .addHeader('Saagie-Realm', getRealm())
            .post(getCredentialsAsJson())
            .build()

        debugRequest(newRequest)
        return newRequest
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

    private boolean checkIfStringIsJson(String query) {
        try {
            JsonOutput.prettyPrint(query)
            return true
        } catch (ignored) {
            return false
        }
    }

    private Request buildMultipartRequestFromQuery(String query, File file, Job job, JobVersion jobVersion) {
        logger.debug('Generating multipart request from query="{}"', query)
        def fileName = file.name
        logger.debug('Using [file={}] for upload', file.absolutePath)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def map = jsonGenerator.toJson(["0": ["variables.file"]])
        def fileBody = RequestBody.create(file, MediaType.parse('application/octet-stream'))

        RequestBody body = new MultipartBody.Builder("--graphql-multipart-upload-boundary-85763456--")
            .setType(MultipartBody.FORM)
            .addFormDataPart("operations", null, RequestBody.create(query, JSON))
            .addFormDataPart("map", null, RequestBody.create(map, JSON))
            .addFormDataPart('0', fileName, fileBody)
            .build()

        Server server = configuration.server
        Request newRequest
        if (server.jwt) {
            logger.debug('Generating graphql request with JWT auth...')
            def realm = server.realm
            def jwtToken = server.token
            newRequest = new Request.Builder()
                .url("${configuration.server.url}/projects/api/platform/${configuration.server.environment}/graphql")
                .addHeader('Cookie', "SAAGIETOKEN$realm=$jwtToken")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "multipart/form-data")
                .post(body)
                .build()
        } else {
            logger.debug('Generating graphql request with basic auth...')
            newRequest = new Request.Builder()
                .url("${configuration.server.url}/api/v1/projects/platform/${configuration.server.environment}/graphql")
                .addHeader('Authorization', getCredentials())
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "multipart/form-data")
                .post(body)
                .build()
        }
        debugRequest(newRequest)
        return newRequest
    }

    Request getPlatformListRequest() {
        Server server = configuration.server
        logger.debug('Generating request in order to get access rights by platforms')
        String jwtToken = server.token

        logger.debug("Using realm=${realm} and jwt=${jwtToken}")
        Request newRequest = new Request.Builder()
            .url("${configuration.server.url}/security/api/groups/authorizations/${configuration.server.environment}/permissions/projects")
            .addHeader('Cookie', "SAAGIETOKEN${realm.toUpperCase()}=${jwtToken}")
            .addHeader('Saagie-Realm', getRealm())
            .get()
            .build()

        debugRequest(newRequest)
        return newRequest
    }

    Request getAllProjectPipelinesRequest() {
        Project project = configuration.project
        logger.debug('Generating getAllProjectPipelinesRequest for project [id={}]', project.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            projectId: project.id
        ])

        def listAllPipelineRequest = gq('''
            query getAllPipelines($projectId: UUID!) {
                pipelines(projectId: $projectId) {
                    id
                    name
                    description
                    versions {
                        number
                        creationDate
                        releaseNote
                        jobs {
                          id
                        }
                        isCurrent
                        isMajor
                        creator
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
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(listAllPipelineRequest)
    }

    Request getListAllProjectGraphPipelinesRequest(){
        Project project = configuration.project
        logger.debug('Generating getAllProjectPipelinesRequest for project [id={}]', project.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            id: project.id
        ])

        def listAllGraphPipelineRequest = gq('''
            query projectQuery($id: UUID!) {
              project(id: $id) {
                id
                pipelines {
                  id
                  name
                  description
                  creationDate
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
                  versions {
                    number
                    isCurrent
                    isMajor
                    releaseNote
                    creationDate
                    creator
                    graph {
                      jobNodes {
                        id
                        job {
                          id
                        }
                        nextNodes
                        position {
                          x
                          y
                        }
                      }
                      conditionNodes {
                        id
                        nextNodesSuccess
                        nextNodesFailure
                        position {
                          x
                          y
                        }
                      }
                    }
                  }
                }
              }
            }
            ''', gqVariables)

        return buildRequestFromQuery(listAllGraphPipelineRequest)
    }

    Request getListAllTechnologiesRequest() {
        logger.debug('Generating getListAllTechnologiesRequest ... ')

        def listAllPipelineRequest = gq('''
            query getAllTechnologies {
                technologies {
                    id
                    label
                    isAvailable
                }
            }
        ''')

        return buildRequestFromQuery(listAllPipelineRequest)
    }

    Request getGroupListRequest() {
        return getPlatformListRequest()
    }

    Request getProjectsCreateRequest() {
        Project project = configuration.project
        logger.debug('Generating getProjectsCreateRequest for creating a new project [name={}]', project.name)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            project: project.toMap()
        ])

        def createProjectRequest = gq('''
            mutation createProjectMutation($project: ProjectInput!) {
                createProject(project: $project) {
                    id
                    name
                    description
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(createProjectRequest)
    }

    Request getJobDetailRequest() {
        Job job = configuration.job
        return getJobDetailRequestFromParam(job.id)
    }

    Request getJobDetailRequestFromParam(jobId) {
        logger.debug('Generating getJobDetailRequest for getting job detail [id={}]', jobId)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([jobId: jobId])

        def getJobDetailRequest = gq('''
           query job($jobId: UUID!) {
                job(id: $jobId) {
                    id
                    name
                    description
                    countJobInstance
                    versions {
                        number
                        creationDate
                        releaseNote
                        runtimeVersion
                        packageInfo {
                            downloadUrl
                        }
                        dockerInfo {
                            image
                            dockerCredentialsId
                        }
                        extraTechnology {
                           language
                           version
                        }
                        commandLine
                        isCurrent
                        isMajor
                        creator
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

        return buildRequestFromQuery(getJobDetailRequest)
    }


    Request getJobDetailRequestFromParamV1(String jobId) {
        Server server = configuration.server
        logger.debug('Generating request in order to get job detail from V1')

        Request newRequest = new Request.Builder()
            .url("${server.url}/manager/api/v1/platform/${server.environment}/job/${jobId}")
            .get()
            .build()

        debugRequest(newRequest)
        return newRequest
    }


    Request getAllVariablesFromV1() {
        Server server = configuration.server
        logger.debug('Generating request in order to get all environment variables from V1')

        Request newRequest = new Request.Builder()
            .url("${server.url}/manager/api/v1/platform/${server.environment}/envvars")
            .get()
            .build()

        debugRequest(newRequest)
        return newRequest
    }


    Request getJobVersionDetailRequest() {
        Project project = configuration.project
        Job job = configuration.job
        logger.debug('Getting jobVersion for job  [id={}] with project Id [id={}]', project.id, job.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([jobId: job.id, number: 1])

        def getJobVersionRequest = gq('''
            query jobVersion($jobId: UUID!, $number: Int!) {
                jobVersion(jobId: $jobId, number: $number) {
                    commandLine
                    dockerInfo {
                        image
                        dockerCredentialsId
                    }
                    releaseNote
                    runtimeVersion
                    packageInfo {
                        name
                        downloadUrl
                    }
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(getJobVersionRequest)
    }

    Request getProjectsUpdateRequest() {
        Project project = configuration.project
        logger.debug('Generating getProjectsUpdateRequest for updating a new project [id={}]', project.id)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            project: project.toMap()
        ])

        def updateProjectRequest = gq('''
            mutation editProjectMutation($project: ProjectEditionInput!) {
                editProject(project: $project) {
                    status
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(updateProjectRequest)
    }

    Request getListVersionForJobRequest(String jobId) {
        Project project = configuration.project
        logger.debug('Generating getListVersionForJobRequest for getting list a new job [id={}]', jobId)

        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        def gqVariables = jsonGenerator.toJson([
            jobId: jobId
        ])

        def listVersionForAJobRequest = gq('''
           query job($jobId: UUID!) {
                job(id: $jobId) {
                    versions {
                        number
                        isCurrent
                    }
                }
            }
        ''', gqVariables)

        return buildRequestFromQuery(listVersionForAJobRequest)
    }

    private Request buildRequestFromQuery(String query, isBearer = false, isGateway = false) {
        logger.debug('Generating request from query="{}"', query)
        RequestBody body = RequestBody.create(query, JSON)
        Server server = configuration.server
        Request newRequest
        String url
        if (isGateway) {
            url = "${removeLastSlash(configuration.server.url)}/gateway/api/graphql"
        } else {
            url = "${removeLastSlash(configuration.server.url)}/projects/api/platform/${configuration.server.environment}/graphql";
        }
        if (server.jwt) {
            logger.debug('Generating graphql request with JWT auth...')
            def realm = server.realm
            def jwtToken = server.token
            newRequest = new Request.Builder()
                .url(url)
                .addHeader('Cookie', "SAAGIETOKEN$realm=$jwtToken")
                .post(body)
                .build()
        } else {
            logger.debug('Generating graphql request with basic auth...')
            newRequest = new Request.Builder()
                .url(url)
                .addHeader('Authorization', !isBearer ? getCredentials() : getBearerCredentials())
                .post(body)
                .build()
        }

        debugRequest(newRequest)
        return newRequest
    }


    void downloadFromHTTPSServer(String urlFrom, String to, OkHttpClient client, String name) {
        try {

            logger.debug("Downloading artifiacts ....")
            Request request = this.buildRequestForFile(urlFrom)
            OkHttpClient clientToDownloadFile = new OkHttpClient.Builder().build();
            def response = clientToDownloadFile.newCall(request).execute()

            File file = new File(to + "/" + name);

            BufferedSink sink = Okio.buffer(Okio.sink(file));
            sink.writeAll(response.body().source());
            sink.close()
        } catch (Exception ex) {
            throw new GradleException(ex.message)
        }
        logger.debug("Artifacts downloaded.")
    }

    Request buildRequestForFile(String url) {
        logger.debug('Generating request for url="{}"', url)
        Server server = configuration.server
        Request newRequest
        logger.debug('Fetching file with basic auth...')

        newRequest = new Request.Builder()
            .url(url)
            .addHeader('Authorization', getCredentials())
            .build()

        debugRequest(newRequest)
        return newRequest
    }

    static String getFileNameFromUrl(String url) {
        return url ? url.substring(url.lastIndexOf('/') + 1, url.length()) : null
    }

    private String getCredentials() {
        Credentials.basic(configuration.server.login, configuration.server.password)
    }

    private RequestBody getCredentialsAsJson() {
        io.saagie.plugin.dataops.models.Credentials credz =
           new io.saagie.plugin.dataops.models.Credentials(login: configuration.server.login, password: configuration.server.password)
        RequestBody.create(new JsonBuilder(credz).toString(), JSON)
    }

    private String getRealm() {
        configuration.server.realm?.toLowerCase()
    }

    private String getBearerCredentials() {
        Server server = configuration.server
        String jwtToken = server.token
        return "Bearer ${jwtToken}"
    }
    // From stackoverflow: https://stackoverflow.com/a/36072704/8543172
    static Map extractProperties(obj) {
        obj.getClass()
            .declaredFields
            .findAll { !it.synthetic }
            .collectEntries { field ->
                [field.name, obj["$field.name"]]
            }
    }

    static boolean distinctValues(int[] arr) {
        Set<String> foundNumbers = new HashSet<String>()
        for (String num : arr) {
            if (foundNumbers.contains(num)) {
                return false
            }
            foundNumbers.add(num)
        }
        return true
    }

    static debugRequest(Request request) {
        logger.debug("====== Request ======")
        logger.debug("${request.method} ${request.url.url().path}")
        logger.debug("Host: ${request.url.url().host}")
        request.headers().names().forEach { logger.debug("${it}: ${request.headers().get(it)}") }
        if (request.body()) {
            logger.debug("Content-Length: ${request.body().contentLength()}")

            final Buffer buffer = new Buffer()
            request.body().writeTo(buffer)
            logger.debug(buffer.readUtf8())
        }
    }

    static debugResponse(Response response, String body) {
        logger.debug("====== Response ======")
        logger.debug("${response.protocol().toString()} ${response.code} ${response.message}")
        response.headers().names().each { logger.debug("${it}: ${response.headers().get(it)}") }
        logger.debug("====== Body ======")
        logger.debug("${body}")
        logger.debug("====== Response End ======")
    }

    static handleErrorClosure = { Logger l, response ->
        l.debug('Checking server response')
        if (response.successful) {
            l.debug('No error in server response.')
            return
        }
        String body = response.body().string()

        debugResponse(response, body)

        String status = "${response.code()}"
        def message = "Error $status when requesting \n$body"
        l.error(message)
        throw new GradleException(message)
    }

    static throwAndLogError(l, message) {
        l.error(message)
        throw new GradleException(message)
    }

    static String convertScheduleV1ToCron(String cronString) {
        if (!cronString) {
            return null
        }
        int startPeriod = cronString.indexOf('/', cronString.indexOf("/") + 1)
        if (!startPeriod) {
            throwAndLogError("Can't parse cronString, couldn t find '/'")
        }
        String startDate = cronString.substring(cronString.indexOf('/') + 1, startPeriod)
        String cronPeriod = cronString.substring(startPeriod + 1, cronString.length())

        Date cronDate = Date.from(ZonedDateTime.parse(startDate, DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(ZoneOffset.UTC)).toInstant())
        PeriodDuration period = PeriodDuration.parse(cronPeriod)
        def minutes, hours, dayOfMonths, months = null
        def time = UnitTime.SECOND.value

        if (period.getPeriod().months) {
            time = UnitTime.MONTH.value
            months = "*/" + period.getPeriod().months
        } else if (time > UnitTime.MONTH.value) {
            months = cronDate.month.toString()
        }

        if (period.getPeriod().days) {
            time = UnitTime.DAYOFMONTH.value
            dayOfMonths = "*/" + period.getPeriod().days
        } else if (time > UnitTime.DAYOFMONTH.value) {
            dayOfMonths = cronDate.day.toString()
        }
        def hoursTest = period.getDuration().toHours()
        if (period.getDuration().toHours()) {
            time = UnitTime.HOUR.value
            hours = "*/" + hoursTest
        } else if (time > UnitTime.HOUR.value) {
            hours = cronDate.hours.toString()
        }

        def minutesTest = period.getDuration().toHours()
        if (period.getDuration().toMinutes() && !period.getDuration().toHours()) {
            time = UnitTime.MINUTE.value
            minutes = "* / " + minutesTest
        } else if (time > UnitTime.MINUTE.value) {
            minutes = cronDate.minutes.toString()
        }

        return generateCronExpression(minutes, hours, dayOfMonths, months)
    }

    static String generateCronExpression(final String minutes, final String hours,
                                         final String dayOfMonth,
                                         final String month) {

        return String.format('%1$s %2$s %3$s %4$s %5$s',
            getValueOrStar(minutes),
            getValueOrStar(hours),
            getValueOrStar(dayOfMonth),
            getValueOrStar(month),
            "*")
    }

    private static getValueOrStar(String value) {
        return value ? value : '*'
    }

    static String removeLastSlash(String url) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url
    }

}
