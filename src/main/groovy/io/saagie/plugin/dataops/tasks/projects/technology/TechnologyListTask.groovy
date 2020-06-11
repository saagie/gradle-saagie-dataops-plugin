package io.saagie.plugin.dataops.tasks.projects.technology

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class TechnologyListTask extends DefaultTask {
    @Internal DataOpsExtension configuration

    @Input String taskName

    @Internal SaagieClient saagieClient

    @TaskAction
    def getProjectTechnologies() {
        saagieClient = new SaagieClient(configuration, taskName)
        def result = saagieClient.listTechnologies()
        logger.quiet(result)
        return result
    }
}
