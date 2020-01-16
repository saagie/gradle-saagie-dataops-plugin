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
            def builder = new JsonBuilder()
            builder.job {
                name exportJob.jobDTO.name
                description exportJob.jobDTO.description
                category exportJob.jobDTO.category
                technology exportJob.jobDTO.technology
                isScheduled exportJob.jobDTO.isScheduled
                cronScheduling exportJob.jobDTO.cronScheduling
                alerting exportJob.jobDTO.alerting
            }
            builder.job {
                jobVersion exportJob.jobVersion
            }
            def json_str = JsonOutput.toJson(builder.toString())
            def json_beauty = JsonOutput.prettyPrint(json_str)
            File jobFile = new File("${inputDire}/${name}/job/${exportJob.job.id}")
            jobFile.write(json_beauty)
            jobFile.close()
            return  "${inputDire}/${name}/";
        }
    }
}

