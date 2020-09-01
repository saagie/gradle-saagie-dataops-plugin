package io.saagie.plugin.dataops.tasks.projects.artifact

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ProjectsExportJobV1Task extends DefaultTask {
	@Input
	DataOpsExtension configuration
	
	@Input
	String taskName
	
	@Internal
	SaagieClient saagieClient
	
	@Internal
	String result
	
	@TaskAction
	def exportProjectJob() {
		saagieClient = new SaagieClient( configuration, taskName )
		
		result = saagieClient.exportArtifactsV1()
		
		logger.quiet( result )
		return result
	}
}
