package io.saagie.plugin.dataops.tasks.projects.jobInstance

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectGetJobInstanceStatusTask extends DefaultTask {
    DataOpsExtension configuration

    @Input String taskName

    @Internal SaagieClient saagieClient

    @TaskAction
    def getJobInstanceStatus() {
        saagieClient = new SaagieClient(configuration, taskName)
        def result = saagieClient.getJobInstanceStatus()
        logger.quiet(result)
        return result
    }
}