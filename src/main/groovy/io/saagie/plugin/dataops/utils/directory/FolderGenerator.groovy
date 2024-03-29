package io.saagie.plugin.dataops.utils.directory

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.AppVersionDTO
import io.saagie.plugin.dataops.models.ExportApp
import io.saagie.plugin.dataops.models.ExportJob
import groovy.json.JsonBuilder
import io.saagie.plugin.dataops.models.ExportPipeline
import io.saagie.plugin.dataops.models.ExportVariables
import io.saagie.plugin.dataops.models.JobVersionDTO
import io.saagie.plugin.dataops.models.PipelineVersionDTO
import io.saagie.plugin.dataops.models.VariableEnvironmentDetailDTO
import io.saagie.plugin.dataops.utils.SaagieUtils
import okhttp3.OkHttpClient
import org.gradle.api.GradleException

class FolderGenerator {

    ExportJob[] exportJobList = []
    ExportPipeline[] exportPipelineList = []
    ExportVariables[] exportVariableList = []
    ExportApp[] exportAppList = []
    def inputDire
    SaagieUtils saagieUtils
    OkHttpClient client
    DataOpsExtension configuration
    String serverUrl
    String projectId
    String environment
    def jobList = []
    String name
    Boolean overwrite
    def sl = File.separator;

    FolderGenerator(inputDire, saagieUtils, client, configuration, name, overwrite) {
        this.inputDire = inputDire
        this.saagieUtils = saagieUtils
        this.client = client
        this.configuration = configuration
        this.serverUrl = configuration.server.url
        this.projectId = configuration.project.id
        this.environment = configuration.server.environment
        this.name = name
        this.overwrite = overwrite
    }

    void generateFolderFromParams(variablesExportedIsEmpty) {
        if (variablesExportedIsEmpty && checkExistenceOfJobsPipelinesVariablesAndApps()) {
            throw new GradleException("Cannot generate zip file")
        }

        if (checkExistenceOfJobsPipelinesVariablesAndApps()) {
            throw new GradleException("jobs, pipelines,variables and apps to be exported cannot be empty at the same time, and cannot generate zip file")
        }
        exportJobList.each { exportJob ->
            generateFolderForJob(exportJob)
        }
        exportPipelineList.each { exportPipeline ->
            generateFolderForPipeline(exportPipeline)
        }

        exportVariableList.each { exportVariable ->
            generateFolderForVariable(exportVariable)
        }

        exportAppList.each { exportApp ->
            generateFolderForApp(exportApp)
        }
    }


    static ArrayList<Map> generateFromJobVersions(ArrayList<JobVersionDTO> versions) {
        def newJobVersionsCollections = versions.collect {
            return generateJobVersion(it, null)
        }
        return newJobVersionsCollections
    }

    static ArrayList<Map> generateFromAppVersions(ArrayList<AppVersionDTO> versions) {
        def newAppVersionsCollections = versions.collect {
            return generateAppVersion(it)
        }
        return newAppVersionsCollections
    }

    void downloadArtifact(
        urlJobIdFolder,
        downloadUrlVersion,
        jobId,
        downloadUrl,
        String extraFolder = '',
        isV1 = false) {
        try {
            def urlFileOflocalPackage = "${urlJobIdFolder}${sl}package"
            if (extraFolder && extraFolder.length() > 0) {
                urlFileOflocalPackage += "${sl}${extraFolder}"
            }
            File localPackage = new File(urlFileOflocalPackage)
            localPackage.mkdirs()
            String urlToDownload = ""

            if (isV1) {
                urlToDownload = SaagieUtils.removeLastSlash(serverUrl) + "/manager/api/v1/platform/${environment}/job/${jobId}/version/${downloadUrlVersion}/binary"
            } else {
                urlToDownload = SaagieUtils.removeLastSlash(serverUrl) +
                    "${sl}api${sl}v1${sl}projects${sl}platform${sl}${environment}${sl}project${sl}" +
                    projectId +
                    "${sl}job${sl}" +
                    jobId +
                    "${sl}version${sl}${downloadUrlVersion}${sl}artifact${sl}" +
                    SaagieUtils.getFileNameFromUrl(downloadUrl)
            }
            saagieUtils.downloadFromHTTPSServer(
                urlToDownload,
                urlFileOflocalPackage,
                client,
                SaagieUtils.getFileNameFromUrl(downloadUrl)
            )
        } catch (IOException e) {
            throw new GradleException(e.message)
        }
    }

    void generateFolderForApp(ExportApp exportApp) {
        def appId = exportApp.appDTO.id
        def urlAppIdFolder = "${inputDire}${sl}${name}${sl}App${sl}${appId}"
        def folder = new File(urlAppIdFolder);

        if (overwrite && folder.exists()) {
            def folderStatus = folder.deleteDir()
            if (!folderStatus) {
                throw new GradleException("Couldn t delete existing folder ${inputDire}${sl}${name}${sl}App")
            }
        }

        if (exportApp.exists()) {
            def createFolderForApp = folder.mkdirs()
            if (createFolderForApp) {
                Map appDetailObject = [
                    name           : exportApp.appDTO.name,
                    category       : exportApp.appDTO.category,
                    technology     : exportApp.appDTO.technology,
                    technologyName : exportApp.appDTO.technologyName,
                    isScheduled    : exportApp.appDTO.isScheduled,
                    storageSizeInMB: exportApp.appDTO.storageSizeInMB,
                    isStreaming    : exportApp.appDTO.isStreaming,
                    description    : exportApp.appDTO.description,
                ]

                if (
                exportApp.appDTO.alerting &&
                    exportApp.appDTO.alerting.emails &&
                    exportApp.appDTO.alerting.emails.size() > 0) {
                    appDetailObject << [*: [
                        alerting: exportApp.appDTO.alerting
                    ]]
                }


                def appVersionDetailJsonObject = generateAppVersion(exportApp.appVersionDTO)
                Map appJsonObject = [
                    app       : appDetailObject,
                    appVersion: appVersionDetailJsonObject
                ]

                if (exportApp.versions) {
                    appJsonObject << [*: [
                        versions: generateFromAppVersions(exportApp.versions)
                    ]]
                }
                def builder = new JsonBuilder(appJsonObject).toPrettyString()
                File appFile = new File("${urlAppIdFolder}${sl}app.json")
                appFile.write(builder)

            } else {
                throw new GradleException("Cannot create directories for the app")
            }
        }
    }

    static Map generateAppVersion(AppVersionDTO appVersionDTO) {
        Map appVersionDetailJsonObject = [:]

        if (appVersionDTO.number) {
            appVersionDetailJsonObject << [*: [
                number: appVersionDTO.number,
            ]]
        }

        if (appVersionDTO.storagePaths) {
            appVersionDetailJsonObject << [*: [
                storagePaths: appVersionDTO.storagePaths,
            ]]
        }

        if (appVersionDTO.exposedPorts) {
            appVersionDetailJsonObject << [*: [
                exposedPorts: appVersionDTO.exposedPorts,
            ]]
        }

        if (appVersionDTO?.resources?.cpu &&
            appVersionDTO?.resources?.disk &&
            appVersionDTO?.resources?.memory) {
            appVersionDetailJsonObject << [*: [
                resources: appVersionDTO.resources,
            ]]
        }

        if (
        appVersionDTO.dockerInfo &&
            appVersionDTO.dockerInfo.image
        ) {
            appVersionDetailJsonObject << [*: [
                dockerInfo: appVersionDTO.dockerInfo,
            ]]
        }

        if (appVersionDTO.releaseNote) {
            appVersionDetailJsonObject << [*: [
                releaseNote: appVersionDTO.releaseNote
            ]]
        }

        return appVersionDetailJsonObject

    }

    void generateFolderForJob(ExportJob exportJob) {
        def jobId = exportJob.jobDTO.id
        def urlJobIdFolder = "${inputDire}${sl}${name}${sl}Job${sl}${jobId}"
        def folder = new File(urlJobIdFolder);

        if (overwrite && folder.exists()) {
            def folderStatus = folder.deleteDir()
            if (!folderStatus) {
                throw new GradleException("Couldn t delete existing folder ${inputDire}${sl}${name}${sl}Job")
            }
        }

        if (exportJob.exists()) {
            def createFolderForJob = folder.mkdirs()
            if (createFolderForJob) {
                Map jobDetailObject = [
                    name          : exportJob.jobDTO.name,
                    category      : exportJob.jobDTO.category,
                    technology    : exportJob.jobDTO.technology,
                    technologyName: exportJob.jobDTO.technologyName,
                    isScheduled   : exportJob.jobDTO.isScheduled,
                    cronScheduling: exportJob.jobDTO.cronScheduling,
                ]

                if (
                exportJob.jobDTO.alerting &&
                    exportJob.jobDTO.alerting.emails &&
                    exportJob.jobDTO.alerting.emails.size() > 0) {
                    jobDetailObject << [*: [
                        alerting: exportJob.jobDTO.alerting
                    ]]
                }

                if (exportJob.jobDTO?.description) {
                    jobDetailObject << [*: [
                        description: exportJob.jobDTO?.description
                    ]]
                }

                def jobVersionDetailJsonObject = generateJobVersion(exportJob.jobVersionDTO, exportJob.downloadUrl)
                Map jobJsonObject = [
                    job       : jobDetailObject,
                    jobVersion: jobVersionDetailJsonObject
                ]

                if (exportJob.versions) {
                    jobJsonObject << [*: [
                        versions: generateFromJobVersions(exportJob.versions)
                    ]]
                }
                def builder = new JsonBuilder(jobJsonObject).toPrettyString()
                File jobFile = new File("${urlJobIdFolder}${sl}job.json")
                jobFile.write(builder)
                if (exportJob.versions && exportJob.versions.size() > 0) {
                    exportJob.versions.each {
                        if (it.packageInfo.name) {
                            downloadArtifact(
                                urlJobIdFolder,
                                it.number,
                                jobId,
                                it.packageInfo?.name,
                                it.number as String,
                                exportJob.isV1
                            )
                        }
                    }
                }

                if (exportJob.downloadUrl && exportJob.downloadUrlVersion) {
                    downloadArtifact(
                        urlJobIdFolder,
                        exportJob.downloadUrlVersion,
                        jobId,
                        exportJob.downloadUrl,
                        "",
                        exportJob.isV1
                    )
                }

            } else {
                throw new GradleException("Cannot create directories for the job")
            }
        }
    }

    static Map generateJobVersion(JobVersionDTO jobVersionDTO, String downloadUrl) {
        Map jobVersionDetailJsonObject = [:]

        if (jobVersionDTO.commandLine) {
            jobVersionDetailJsonObject << [*: [
                commandLine: jobVersionDTO.commandLine,
            ]]
        }

        if (jobVersionDTO.number) {
            jobVersionDetailJsonObject << [*: [
                number: jobVersionDTO.number,
            ]]
        }

        if (
        jobVersionDTO.dockerInfo &&
            jobVersionDTO.dockerInfo.image
        ) {
            jobVersionDetailJsonObject << [*: [
                dockerInfo: jobVersionDTO.dockerInfo,
            ]]
        }

        if (jobVersionDTO.runtimeVersion) {
            jobVersionDetailJsonObject << [*: [
                runtimeVersion: jobVersionDTO.runtimeVersion
            ]]
        }

        if (jobVersionDTO.releaseNote) {
            jobVersionDetailJsonObject << [*: [
                releaseNote: jobVersionDTO.releaseNote
            ]]
        }

        if (
        jobVersionDTO.extraTechnology &&
            jobVersionDTO.extraTechnology.language
        ) {
            jobVersionDetailJsonObject << [*: [
                extraTechnology: jobVersionDTO.extraTechnology
            ]]
        }

        return jobVersionDetailJsonObject

    }

    void generateFolderForPipeline(ExportPipeline exportPipeline) {

        def pipelineId = exportPipeline.pipelineDTO.id
        def urlPipelineIdFolder = "${inputDire}${sl}${name}${sl}Pipeline${sl}${pipelineId}"
        def folder = new File(urlPipelineIdFolder);

        if (overwrite && folder.exists()) {
            def folderStatus = folder.deleteDir()
            if (!folderStatus) {
                throw new GradleException("Couldn t delete existing folder ${inputDire}${sl}${name}${sl}Pipeline")
            }
        }

        if (exportPipeline.exists()) {
            def createFolderForPipeLine = folder.mkdirs()
            if (createFolderForPipeLine && jobList) {

                Map pipelineDetailJson = [
                    name          : exportPipeline.pipelineDTO?.name,
                    description   : exportPipeline.pipelineDTO?.description,
                    isScheduled   : exportPipeline.pipelineDTO?.isScheduled,
                    cronScheduling: exportPipeline.pipelineDTO?.cronScheduling,
                ]

                if (
                exportPipeline.pipelineDTO?.description) {
                    pipelineDetailJson << [*: [
                        description: exportPipeline.pipelineDTO?.description
                    ]]
                }

                if (
                exportPipeline.pipelineDTO?.alerting &&
                    exportPipeline.pipelineDTO?.alerting?.emails &&
                    exportPipeline.pipelineDTO?.alerting?.emails.size() > 0) {
                    pipelineDetailJson << [*: [
                        alerting: exportPipeline.pipelineDTO?.alerting
                    ]]
                }

                def pipelineVersionDetailJson = generatePipelineVersion(exportPipeline.pipelineVersionDTO)

                def pipelineBody = [
                    pipeline       : pipelineDetailJson,
                    pipelineVersion: pipelineVersionDetailJson
                ]

                if (exportPipeline.versions) {
                    pipelineBody << [*: [
                        versions: generateFromPipelineVersions(exportPipeline.versions)
                    ]]
                }

                def builder = new JsonBuilder(pipelineBody).toPrettyString()
                File jobFile = new File("${urlPipelineIdFolder}${sl}pipeline.json")
                jobFile.write(builder)
            } else {
                throw new GradleException("Cannot create directories for the pipeline")
            }
        }
    }

    void generateFolderForVariable(ExportVariables exportVariable) {

        def variableId = exportVariable.variableEnvironmentDTO.name
        def urlVariableIdFolder = "${inputDire}${sl}${name}${sl}Variable${sl}${variableId}"
        def folder = new File(urlVariableIdFolder);


        def createFolderForVariable = folder.mkdirs()
        if (createFolderForVariable) {

            Map variablebDetailObject = generateBodyEnvironmentVariable(exportVariable.variableEnvironmentDTO.name, exportVariable.variableEnvironmentDTO.variableDetail)

            if (
            exportVariable.variableEnvironmentDTO?.hasProperty('overridenValues') && exportVariable?.variableEnvironmentDTO?.overridenValues.size() > 0) {
                ArrayList<Map> overridenValues = []
                exportVariable.variableEnvironmentDTO.overridenValues.forEach { it ->
                    overridenValues.add(generateBodyEnvironmentVariable(null, it))
                }
                variablebDetailObject << [*: [
                    overridenValues: overridenValues
                ]]
            }

            def builder = new JsonBuilder(variablebDetailObject).toPrettyString()
            File variableFile = new File("${urlVariableIdFolder}${sl}variable.json")
            variableFile.write(builder)
        } else {
            throw new GradleException("Cannot create directories for the variable")
        }

    }

    Map generateBodyEnvironmentVariable(String name, VariableEnvironmentDetailDTO variableDetail) {
        Map variableDetailObject = [
            scope      : variableDetail.scope,
            value      : variableDetail.value,
            description: variableDetail.description,
            isPassword : variableDetail.isPassword,
        ]

        if (name) {
            variableDetailObject << [*: [
                name: name,
            ]]
        }

        if (variableDetail.id) {
            variableDetailObject << [*: [
                id: variableDetail.id,
            ]]
        }

        return variableDetailObject
    }

    ArrayList<Map> generateFromPipelineVersions(versions) {
        def newPipelineVersionsCollections = versions.collect {
            return generatePipelineVersion(it)
        }
        return newPipelineVersionsCollections
    }

    Map generatePipelineVersion(PipelineVersionDTO pipelineVersionDTO) {
        Map pipelineVersionDetailJson = [:]

        if (pipelineVersionDTO?.jobs && !pipelineVersionDTO.jobs.isEmpty()) {
            def jobForPipeVersionArray = []

            pipelineVersionDTO?.jobs?.each { jobId ->
                jobList.each { job ->
                    if (jobId && job && jobId.id == job.id) {
                        jobForPipeVersionArray.add([
                            id  : job.id,
                            name: job.name
                        ])
                    }
                }
            }

            pipelineVersionDetailJson << [*: [
                jobs: jobForPipeVersionArray
            ]]
        }

        if (pipelineVersionDTO?.graph?.jobNodes && !pipelineVersionDTO.graph.jobNodes.isEmpty()) {
            def jobNodesArray = []
            pipelineVersionDTO.graph.jobNodes.each {
                jobNodesArray.add(
                    [
                        id: it.id,
                        job: [
                            id: it.job.id,
                            name: jobList.find { job -> job.id == it.job.id }
                        ],
                        position: it.position,
                        nextNodes: it.nextNodes
                    ]
                )
            }
            Map graphMap = [
                jobNodes: jobNodesArray,
                conditionNodes:  pipelineVersionDTO.graph.conditionNodes
            ]
            pipelineVersionDetailJson.put('graph', graphMap)
        }

        if (pipelineVersionDTO?.releaseNote) {
            pipelineVersionDetailJson << [*: [
                releaseNote: pipelineVersionDTO?.releaseNote
            ]]
        }

        if (pipelineVersionDTO?.number) {
            pipelineVersionDetailJson << [*: [
                number: pipelineVersionDTO?.number
            ]]
        }
        return pipelineVersionDetailJson
    }

    boolean checkExistenceOfJobsPipelinesVariablesAndApps() {
        return !exportJobList.length && !exportPipelineList.length && !exportVariableList.length && !exportAppList.length
    }

    static extractNameFileFromUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1, url.length())
    }

    static extractNameFileFromUrlWithoutExtension(String text) {
        if (text.lastIndexOf('.') < 0) {
            throw new GradleException("The file you want to export doesn't contain an extension")
        }
        return text.replace(text.substring(text.lastIndexOf('.'), text.length()), "")
    }

    static extractUrlWithoutFileName(String urlString) {
        return urlString.replace(extractNameFileFromUrl(urlString), "")
    }
}

