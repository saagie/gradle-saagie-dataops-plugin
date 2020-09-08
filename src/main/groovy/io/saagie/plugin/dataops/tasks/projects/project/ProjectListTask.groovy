package io.saagie.plugin.dataops.tasks.projects.project

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectListTask extends DefaultTask {
	@Input
	DataOpsExtension configuration
	
	@Input
	String taskName
	
	@Internal
	SaagieClient saagieClient
	
	@Internal
	String result
	
	@TaskAction
	def getProjects() {
		saagieClient = new SaagieClient(configuration, taskName)
		result = saagieClient.getProjects()
		logger.quiet(result)
		return result
	}
}
