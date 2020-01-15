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

    String generateFolder(name, override) {
        def folder = new File("${inputDire}/${name}/job");
        if (override && folder.exists()) {
            def folderStatus = folder.deleteDir()
            if(!folderStatus) {
                throw new GradleException("Couldn t delete existing folder ${inputDire}/${name}/job")
            }
        }
        if(exportJob.exists()) {
            folder.mkdir()
            def builder = new JsonBuilder()
            def root = builder.job {
                job {
                    name exportJob.job.name
                    description exportJob.job.description
                    category exportJob.job.category
                    technology exportJob.job.technology
                    isScheduled exportJob.job.isScheduled
                    cronScheduling exportJob.job.cronScheduling
                    alerting exportJob.job.alerting
                }
                jobVersion exportJob.jobVersion
            }
            def json_str = JsonOutput.toJson(root)
            def json_beauty = JsonOutput.prettyPrint(json_str)
            File jobFile = new File("${inputDire}/${name}/job/${exportJob.job.id}")
            jobFile.write(json_beauty)
            jobFile.close()
            return  "${inputDire}/${name}/";
        }
    }
}

