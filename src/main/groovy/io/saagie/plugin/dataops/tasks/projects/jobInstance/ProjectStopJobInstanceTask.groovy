package io.saagie.plugin.dataops.tasks.projects.jobInstance

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectStopJobInstanceTask extends DefaultTask {
    @Internal DataOpsExtension configuration

    @Input String taskName

    @Internal SaagieClient saagieClient

    @TaskAction
    def stopProjectJobInstance() {
        saagieClient = new SaagieClient(configuration, taskName)
        def result = saagieClient.stopJobInstance()
        logger.quiet(result)
        return result
    }
}
