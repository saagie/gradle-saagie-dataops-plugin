package io.saagie.plugin.dataops.tasks.projects.job

import groovy.transform.TypeChecked
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import io.saagie.plugin.dataops.models.Server
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

@TypeChecked
class ProjectCreateJobTask extends DefaultTask {
    DataOpsExtension configuration

    @Input String taskName

    @Internal SaagieClient saagieClient

    String result

    @TaskAction
    def createProjectJob() {
        Server server = configuration.server
        saagieClient = new SaagieClient(configuration, taskName)

        def response
        if (server.useLegacy) {
            logger.info("Using deprecated version of projectsCreateJob")
            response = saagieClient.createProjectJob()
        } else {
            response = saagieClient.createProjectJobWithGraphQL()
        }

        logger.quiet(response)

        result = response

        //TODO find a way to reset the configuration
    }
}