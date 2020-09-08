package io.saagie.plugin.dataops.tasks.projects.pipelineInstance

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectStopPipelineInstanceTask extends DefaultTask {
	@Input
	DataOpsExtension configuration
	
	@Input
	String taskName
	
	@Internal
	SaagieClient saagieClient
	
	@Internal
	String result
	
	@TaskAction
	def stopProjectPipelineInstance() {
		saagieClient = new SaagieClient(configuration, taskName)
		result = saagieClient.stopPipelineInstance()
		logger.quiet(result)
		return result
	}
}
