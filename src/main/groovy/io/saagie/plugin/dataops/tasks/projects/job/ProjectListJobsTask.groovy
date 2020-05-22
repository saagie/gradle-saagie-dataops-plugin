package io.saagie.plugin.dataops.tasks.projects.job

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectListJobsTask extends DefaultTask {
    @Input DataOpsExtension configuration

    @Input String taskName

    @Internal SaagieClient saagieClient

    @TaskAction
    def getProjectJobs() {
        saagieClient = new SaagieClient(configuration, taskName)
        def result = saagieClient.getProjectJobs()
        logger.quiet(result)
        return result
    }
}
