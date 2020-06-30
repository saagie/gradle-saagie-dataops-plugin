package io.saagie.plugin.dataops.tasks.projects.artifact

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskOutputs

class ProjectsExportJobTask extends DefaultTask {
	
	@Input
	DataOpsExtension configuration
	
	@Input
	String taskName
	
	@Internal
	SaagieClient saagieClient
	
	@TaskAction
	def exportProjectJob() {
		saagieClient = new SaagieClient( configuration, taskName )
		
		def result = saagieClient.exportArtifacts()
		logger.quiet( result )
		return result
	}
	
}
