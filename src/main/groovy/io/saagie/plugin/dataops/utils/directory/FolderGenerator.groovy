package io.saagie.plugin.dataops.utils.directory

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.ExportJobs
import groovy.json.JsonBuilder
import io.saagie.plugin.dataops.models.ExportPipeline
import io.saagie.plugin.dataops.utils.SaagieUtils
import okhttp3.OkHttpClient
import org.gradle.api.GradleException

class FolderGenerator {

    ExportJobs[] exportJobList = []
    ExportPipeline[] exportPipelineList = []
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



    void generateFolderForJob(ExportJobs exportJob) {
        def jobId = exportJob.jobDTO.id
        def urlJobIdFolder = "${inputDire}${sl}${name}${sl}Job${sl}${jobId}"
        def folder = new File(urlJobIdFolder);

        if (overwrite && folder.exists()) {
            def folderStatus = folder.deleteDir()
            if(!folderStatus) {
                throw new GradleException("Couldn t delete existing folder ${inputDire}${sl}${name}${sl}Job")
            }
        }

        if(exportJob.exists()) {
            def createFolderForJob = folder.mkdirs()
            if(createFolderForJob) {
                Map jobDetailObject = [
                    name: exportJob.jobDTO.name,
                    category: exportJob.jobDTO.category,
                    technology: exportJob.jobDTO.technology,
                    isScheduled: exportJob.jobDTO.isScheduled,
                    cronScheduling: exportJob.jobDTO.cronScheduling,
                ]

                Map jobVersionDetailJsonObject = [:]

                if(exportJob.jobVersionDTO.commandLine) {
                    jobVersionDetailJsonObject << [*: [
                        commandLine: exportJob.jobVersionDTO.commandLine,
                    ]]
                }

                if (
                    exportJob.jobVersionDTO.dockerInfo &&
                    exportJob.jobVersionDTO.dockerInfo.image
                ) {
                    jobVersionDetailJsonObject << [*: [
                        dockerInfo: exportJob.jobVersionDTO.dockerInfo,
                    ]]
                }

                if(exportJob.jobVersionDTO.runtimeVersion) {
                    jobVersionDetailJsonObject << [*: [
                        runtimeVersion:  exportJob.jobVersionDTO.runtimeVersion
                    ]]
                }

                if (
                    exportJob.jobDTO.alerting &&
                    exportJob.jobDTO.alerting.emails &&
                    exportJob.jobDTO.alerting.emails.size() > 0) {
                    jobDetailObject << [*: [
                        alerting: exportJob.jobDTO.alerting
                    ]]
                }

                if ( exportJob.jobDTO?.description ) {
                    jobDetailObject << [*: [
                        description: exportJob.jobDTO?.description
                    ]]
                }

                if(
                    exportJob.jobVersionDTO.packageInfo &&
                    exportJob.jobVersionDTO.packageInfo.name &&
                    exportJob.downloadUrl
                ) {
                    jobVersionDetailJsonObject << [ * : [
                        packageInfo: [
                            downloadUrl: exportJob.downloadUrl,
                            name: exportJob.jobVersionDTO.packageInfo.name,
                        ]
                    ]]
                }



                Map jobJsonObject = [
                    job: jobDetailObject,
                    jobVersion: jobVersionDetailJsonObject
                ]
                def builder = new JsonBuilder(jobJsonObject).toPrettyString()
                File jobFile = new File("${urlJobIdFolder}${sl}job.json")
                jobFile.write(builder)
                if(exportJob.downloadUrl && exportJob.downloadUrlVersion){
                    try {
                        File localPackage = new File("${urlJobIdFolder}${sl}package")
                        localPackage.mkdirs()
                        String urlToDownload = ""
                        String packageUrl = "${urlJobIdFolder}${sl}package";

                        if(exportJob.isV1) {
                            urlToDownload = SaagieUtils.removeLastSlash(serverUrl) +"/manager/api/v1/platform/${environment}/job/${jobId}/version/${exportJob.downloadUrlVersion}/binary"
                        } else {
                            urlToDownload = SaagieUtils.removeLastSlash(serverUrl) +
                                "${sl}api${sl}v1${sl}projects${sl}platform${sl}${environment}${sl}project${sl}"+
                                projectId +
                                "${sl}job${sl}"+
                                jobId +
                                "${sl}version${sl}${exportJob.downloadUrlVersion}${sl}artifact${sl}"+
                                SaagieUtils.getFileNameFromUrl(exportJob.downloadUrl)
                        }
                        saagieUtils.downloadFromHTTPSServer(
                            urlToDownload,
                            packageUrl,
                            client,
                            SaagieUtils.getFileNameFromUrl(exportJob.downloadUrl)
                        )
                     } catch (IOException e) {
                        throw new GradleException(e.message)
                    }
                }

            } else {
                throw new GradleException("Cannot create directories for the job")
            }
        }
    }

    void generateFolderForPipeline(ExportPipeline exportPipeline) {

        def pipelineId = exportPipeline.pipelineDTO.id
        def urlPipelineIdFolder = "${inputDire}${sl}${name}${sl}Pipeline${sl}${pipelineId}"
        def folder = new File(urlPipelineIdFolder);


        if (overwrite && folder.exists()) {
            def folderStatus = folder.deleteDir()
            if(!folderStatus) {
                throw new GradleException("Couldn t delete existing folder ${inputDire}${sl}${name}${sl}Pipeline")
            }
        }

        if(exportPipeline.exists()) {
            def createFolderForPipeLine = folder.mkdirs()
            if(createFolderForPipeLine && jobList) {
                def jobForPipeVersionArray = []
                if(exportPipeline && exportPipeline.pipelineVersionDTO && exportPipeline.pipelineVersionDTO.jobs) {
                    jobList.each { job ->
                        exportPipeline.pipelineVersionDTO?.jobs?.each { jobId ->
                            def element = null

                            if(jobId.id == job.id){
                                jobForPipeVersionArray.add([
                                    id : job.id,
                                    name: job.name
                                ])
                            }
                        }
                    }
                }
                Map pipelineDetailJson = [
                    name : exportPipeline.pipelineDTO?.name,
                    description : exportPipeline.pipelineDTO?.description,
                    isScheduled : exportPipeline.pipelineDTO?.isScheduled,
                    cronScheduling : exportPipeline.pipelineDTO?.cronScheduling,
                ]

                Map pipelineVersionDetailJson = [
                    jobs: jobForPipeVersionArray
                ]

                if ( exportPipeline.pipelineDTO?.description ) {
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

                if(
                    exportPipeline.pipelineVersionDTO?.releaseNote
                ) {
                    pipelineVersionDetailJson << [ * : [
                        releaseNote: exportPipeline.pipelineVersionDTO?.releaseNote
                    ]]
                }

                if(
                    exportPipeline.pipelineVersionDTO?.releaseNote
                ) {
                    pipelineVersionDetailJson << [ * : [
                        releaseNote: exportPipeline.pipelineVersionDTO?.releaseNote
                    ]]
                }
                def builder = new JsonBuilder([
                    pipeline: pipelineDetailJson,
                    pipelineVersion: pipelineVersionDetailJson
                ]).toPrettyString()
                File jobFile = new File("${urlPipelineIdFolder}${sl}pipeline.json")
                jobFile.write(builder)
            } else {
                throw new GradleException("Cannot create directories for the pipeline")
            }
        }
    }

    void generateFolderFromParams() {
        if(!exportJobList.length && ! exportPipelineList.length) {
            throw new GradleException("jobs and pipelines to be exported can t be empty at the same time")
        }
        exportJobList.each { exportJob ->
            generateFolderForJob(exportJob)
        }
        exportPipelineList.each { exportPipeline ->
            generateFolderForPipeline(exportPipeline)
        }
    }

    static extractNameFileFromUrl(String url){
        return url.substring( url.lastIndexOf('/')+1, url.length() )
    }

    static extractNameFileFromUrlWithoutExtension(String text){
        if(text.lastIndexOf('.') < 0){
            throw new GradleException("The file you want to export doesn't contain an extension")
        }
         return text.replace(text.substring(text.lastIndexOf('.'),text.length()), "")
    }
    static extractUrlWithoutFileName(String urlString) {
        return urlString.replace(extractNameFileFromUrl(urlString),"");

    }
}

