package io.saagie.plugin.dataops.tasks.projects.job

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import io.saagie.plugin.dataops.models.Job
import io.saagie.plugin.dataops.models.JobVersion
import io.saagie.plugin.dataops.models.Server
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class ProjectUpgradeJobTask extends DefaultTask {
    @Input DataOpsExtension configuration

    @Input String taskName

    @Internal SaagieClient saagieClient
    
    String result
    
    @TaskAction
    def upgradeProjectJob() {
        
        saagieClient = new SaagieClient(configuration, taskName)
        def response = saagieClient.upgradeProjectJobWithGraphQL()
        logger.quiet(response)
        result = saagieClient.slurper.parseText(response)
        
        return result
    }
}
