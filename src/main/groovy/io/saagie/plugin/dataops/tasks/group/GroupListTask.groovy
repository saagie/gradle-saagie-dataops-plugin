package io.saagie.plugin.dataops.tasks.group

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class GroupListTask extends DefaultTask {
	@Input
	DataOpsExtension configuration
	
	@Input
	String taskName
	
	@Internal
	SaagieClient saagieClient
	
	@Internal
	def result
	
	@TaskAction
	def listGroups() {
		saagieClient = new SaagieClient( configuration, taskName )
		result = saagieClient.listGroups()
		logger.quiet( result )
		return result
	}
}
