package io.saagie.plugin.dataops.utils.directory

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.ExportJob
import groovy.json.JsonBuilder
import io.saagie.plugin.dataops.models.ExportPipeline
import io.saagie.plugin.dataops.utils.SaagieUtils
import okhttp3.OkHttpClient
import org.gradle.api.GradleException

class FolderGenerator {

    ExportJob[] exportJobList
    ExportPipeline[] exportPipelineList
    def inputDire
    SaagieUtils saagieUtils
    OkHttpClient client
    DataOpsExtension configuration
    String serverUrl
    String projectId
    String environment
    String name
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



    void generateFolderForJob(ExportJob exportJob) {
        def jobId = exportJob.jobDTO.id
        def sl = File.separator;
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
                def builder = new JsonBuilder([
                    job: [
                        name: exportJob.jobDTO.name,
                        description: exportJob.jobDTO.description,
                        category: exportJob.jobDTO.category,
                        technology: exportJob.jobDTO.technology,
                        isScheduled: exportJob.jobDTO.isScheduled,
                        cronScheduling: exportJob.jobDTO.cronScheduling,
                        alerting: exportJob.jobDTO.alerting
                    ],
                    jobVersion: [
                        commandLine: exportJob.jobVersionDTO.commandLine,
                        dockerInfo: exportJob.jobVersionDTO.dockerInfo,
                        runtimeVersion: exportJob.jobVersionDTO.runtimeVersion,
                        releaseNote: exportJob.jobVersionDTO.releaseNote,
                        packageInfo: [
                            downloadUrl: exportJob.downloadUrl,
                            name: exportJob.jobVersionDTO.packageInfo.name,
                        ]
                    ]
                ]).toPrettyString()
                File jobFile = new File("${urlJobIdFolder}${sl}job.json")
                jobFile.write(builder)
                try {
                    File localPackage = new File("${urlJobIdFolder}${sl}package")
                    localPackage.mkdirs()
                    saagieUtils.downloadFromHTTPSServer(
                        SaagieUtils.removeLastSlash(serverUrl) +
                            "${sl}api${sl}v1${sl}projects${sl}platform${sl}${environment}${sl}project${sl}"+
                            projectId +
                            "${sl}job${sl}"+
                            jobId +
                            "${sl}version${sl}${exportJob.downloadUrlVersion}${sl}artifact${sl}"+
                            SaagieUtils.getFileNameFromUrl(exportJob.downloadUrl),
                        "${urlJobIdFolder}${sl}package",
                        client
                    )
                } catch (IOException e) {
                    throw new GradleException(e.message)
                }
            } else {
                throw new GradleException("Cannot create directories for the job")
            }
        }
    }

    void generateFolderForPipeline(ExportPipeline exportPipeline) {

        def pipelineId = exportPipeline.pipelineDTO.id
        def sl = File.separator;
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
            if(createFolderForPipeLine) {
                def builder = new JsonBuilder([
                    pipeline: [
                        name : exportPipeline.pipelineDTO.name,
                        description : exportPipeline.pipelineDTO.description,
                        isScheduled : exportPipeline.pipelineDTO.isScheduled,
                        cronScheduling : exportPipeline.pipelineDTO.cronScheduling,
                        alerting : exportPipeline.pipelineDTO.alerting
                    ],
                    pipelineVersion: [
                        releaseNote: exportPipeline.pipelineVersionDTO.releaseNote,
                        jobs: exportPipeline.pipelineVersionDTO.jobs
                    ]
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
        exportJobList.each { exportJob -> {
            generateFolderForJob(exportJob)
        }}
        exportPipelineList.each { exportPipeline -> {
            generateFolderForPipeline(exportPipeline)
        }}
    }
}

