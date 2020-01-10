package io.saagie.plugin.dataops.utils.directory

import io.saagie.plugin.dataops.models.ExportJob
import groovy.json.JsonBuilder;
class FolderGenerator {
    ExportJob exportJob;
    def GenerateFolder(inputDire, name) {
        def folder = new File("${inputDire}/${name}/job");
        if(!folder.exists() && exportJob.exists()) {
            folder.mkdir()
            def object = jsonSlurper.parseText '''
                { "job": ${ JsonOutput.toJson(exportJob.job)},
                  "jobVersion": ${exportJob.jobVersion}
                }'''
            def builder = new JsonBuilder()
            def root = builder.job {
                job {
                    firstName 'Guillame'
                    lastName 'Laforge'
                }
                jobVersion {

                }
            }
            def json_str = JsonOutput.toJson(exportJob.job)
            def json_beauty = JsonOutput.prettyPrint(json_str)
            File jobFile = new File("${inputDire}/${name}/job/${job.id}")
            jobFile.write(json_beauty)
        }
    }
}

