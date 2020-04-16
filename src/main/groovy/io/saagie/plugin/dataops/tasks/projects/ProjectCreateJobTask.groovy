package io.saagie.plugin.dataops.tasks.projects

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
    @Input DataOpsExtension configuration

    @Input String taskName

    @Internal SaagieClient saagieClient

    String response

    @TaskAction
    def createProjectJob() {
        Server server = configuration.server
        saagieClient = new SaagieClient(configuration, taskName)

        def result
        if (server.useLegacy) {
            logger.info("Using deprecated version of projectsCreateJob")
            result = saagieClient.createProjectJob()
        } else {
            result = saagieClient.createProjectJobWithGraphQL()
        }

        logger.quiet(result)
        response = result
    }
}
