package io.saagie.plugin.dataops.utils.directory

import io.saagie.plugin.dataops.models.Job

class FolderGenerator {
    Job job;
    def GenerateFolder(inputDire, name) {
        def folder = new File("${inputDire}/${name}");
        if(!folder.exists()) {
            folder.mkdir()
        }
        def jobFolder = new File("${inputDire}/${name}/job")
        jobFolder.mkdir()
        if(job && job.id) {
            def jobIdFolder = new File("${inputDire}/${name}/job/${job.id}")
            def json_str = JsonOutput.toJson(job)
            def json_beauty = JsonOutput.prettyPrint(json_str)
            File jobFile = new File("${inputDire}/${name}/job/${job.id}")
        }

    }
}

