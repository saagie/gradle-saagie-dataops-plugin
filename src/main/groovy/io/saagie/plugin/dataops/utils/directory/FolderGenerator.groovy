package io.saagie.plugin.dataops.utils.directory

import groovy.json.JsonSlurper
import io.saagie.plugin.dataops.models.ExportJob
import groovy.json.JsonBuilder
import io.saagie.plugin.dataops.utils.SaagieUtils
import okhttp3.OkHttpClient
import org.gradle.api.GradleException;
class FolderGenerator {

    ExportJob exportJob
    def inputDire
    SaagieUtils saagieUtils
    OkHttpClient client
    FolderGenerator(exportJob, inputDire, saagieUtils, client) {
        this.exportJob = exportJob
        this.inputDire = inputDire
        this.saagieUtils = saagieUtils
        this.client = client
    }

    void generateFolder(name, overwrite, String serverUrl, jobId) {
        def folder = new File("${inputDire}/${name}/Job/${jobId}");
        def sl = File.separator;
        def urlJobIdFolder = "${inputDire}/${name}/Job/${jobId}"
        if (overwrite && folder.exists()) {
            def folderStatus = folder.deleteDir()
            if(!folderStatus) {
                throw new GradleException("Couldn t delete existing folder ${inputDire}${sl}${name}${sl}job")
            }
        }
        // TODO add the condition for the overwrite

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
                    JobVersion: [
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
                        SaagieUtils.removeLastSlash(serverUrl) + exportJob.downloadUrl,
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
}

