package io.saagie.plugin.dataops.tasks.projects.jobInstance

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectGetJobInstanceStatusTask extends DefaultTask {
	@Input
	DataOpsExtension configuration
	
	@Input
	String taskName
	
	@Internal
	SaagieClient saagieClient
	
	String result
	
	@TaskAction
	def getJobInstanceStatus() {
		
		saagieClient = new SaagieClient(configuration, taskName)
		def response = saagieClient.getJobInstanceStatus()
		logger.quiet(response)
		result = saagieClient.slurper.parseText(response)
		
		return result
	}
}
