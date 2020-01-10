package io.saagie.plugin.dataops.utils.directory

import io.saagie.plugin.dataops.models.Job
import io.saagie.plugin.dataops.models.JobVersion

class FolderGenerator {
    Job job;
    JobVersion jobVersion
    def GenerateFolder(inputDire, name) {
        def folder = new File("${inputDire}/${name}/job");
        if(!folder.exists() && job && job.id) {
            folder.mkdir()
            def json_str = JsonOutput.toJson(job)
            def json_beauty = JsonOutput.prettyPrint(json_str)
            File jobFile = new File("${inputDire}/${name}/job/${job.id}")
            jobFile.write(json_beauty)
        }
    }
}

