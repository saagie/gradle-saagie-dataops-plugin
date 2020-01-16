package io.saagie.plugin.dataops.utils.directory

import io.saagie.plugin.dataops.models.ExportJob
import groovy.json.JsonBuilder
import org.gradle.api.GradleException;
class FolderGenerator {
    ExportJob exportJob
    def inputDire

    FolderGenerator(exportJob, inputDire) {
        this.exportJob = exportJob
        this.inputDire = inputDire
    }

    String generateFolder(name, overwrite) {
        def folder = new File("${inputDire}/${name}/job");
        if (overwrite && folder.exists()) {
            def folderStatus = folder.deleteDir()
            if(!folderStatus) {
                throw new GradleException("Couldn t delete existing folder ${inputDire}/${name}/job")
            }
        }
        // TODO add the condition for the overwrite

        if(exportJob.exists()) {
            folder.mkdir()
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
                    packageInfo: exportJob.jobVersionDTO.packageInfo,
                ]
            ]).toPrettyString()
            File jobFile = new File("${inputDire}/${name}/job/${exportJob.jobDTO.id}")
            jobFile.mkdirs()
            if( jobFile.canWrite()) {
                def mkdirs = jobFile.mkdirs()
                if (mkdirs) {
                    jobFile.write(builder)
                    jobFile.close()
                    return  "${inputDire}/${name}/";
                }
                else {
                    throw new GradleException("Cannot create directories")
                }
            } else {
                throw new GradleException("don't have permissions to create file for the job")
            }

        }
    }
}

