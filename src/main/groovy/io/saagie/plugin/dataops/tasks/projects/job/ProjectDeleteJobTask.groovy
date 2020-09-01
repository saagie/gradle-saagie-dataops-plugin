package io.saagie.plugin.dataops.tasks.projects.job

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectDeleteJobTask extends DefaultTask {
    @Input DataOpsExtension configuration

    @Input String taskName

    @Internal SaagieClient saagieClient
    
    @Input
    String result
    
    @TaskAction
    def deleteProjectJob() {
        saagieClient = new SaagieClient(configuration, taskName)
        result = saagieClient.deleteProjectJob()
        logger.quiet(result)
        return result
    }
}
