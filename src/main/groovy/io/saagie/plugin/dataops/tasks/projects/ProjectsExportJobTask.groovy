package io.saagie.plugin.dataops.tasks.projects

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectsExportJobTask extends DefaultTask{

    @Input DataOpsExtension configuration

    @Input String taskName

    @Internal SaagieClient saagieClient

    @TaskAction
    def ExportJob() {
        saagieClient = new SaagieClient(configuration, taskName)

        def result = saagieClient.exportJob()
        logger.debug("results {}", result)
        return result
    }
}
