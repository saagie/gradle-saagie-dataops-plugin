package io.saagie.plugin.dataops.utils.directory

import io.saagie.plugin.dataops.models.ExportJob
import groovy.json.JsonBuilder;
class FolderGenerator {
    ExportJob exportJob;
    def GenerateFolder(inputDire, name) {
        def folder = new File("${inputDire}/${name}/job");
        if(!folder.exists() && exportJob.exists()) {
            folder.mkdir()
            def builder = new JsonBuilder()
            def root = builder.job {
                job {
                    name exportJob.job.name
                    description exportJob.job.name
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
        }
    }
}

