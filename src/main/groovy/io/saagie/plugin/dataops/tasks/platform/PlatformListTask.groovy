package io.saagie.plugin.dataops.tasks.platform

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input

class PlatformListTask extends DefaultTask {
    @Internal DataOpsExtension configuration

    @Input String taskName

    @Internal SaagieClient saagieClient

    @TaskAction
    def archiveProjectJob() {
        saagieClient = new SaagieClient(configuration, taskName)
        def result = saagieClient.listPlatforms()
        logger.quiet(result)
        return result
    }
}
