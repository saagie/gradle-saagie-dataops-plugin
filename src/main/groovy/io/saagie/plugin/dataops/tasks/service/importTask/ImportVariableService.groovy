package io.saagie.plugin.dataops.tasks.service.importTask

import io.saagie.plugin.dataops.models.VariableMapper
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class ImportVariableService {
	static final Logger logger = Logging.getLogger(ImportVariableService.class)
	
	def static importAndCreateVariables( Map variables, Closure mapClosure ) {
		variables.each { variable ->
			def variableId = variable.key
			Map variableValue = variable.value.configOverride
			def newVariableConfig = [
					* : variableValue
			]
			
			VariableMapper newMappedVariable = new VariableMapper()
			newMappedVariable.name = newVariableConfig.name
			newMappedVariable.scope = newVariableConfig.scope
			if(newVariableConfig.id && newVariableConfig.id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
				newMappedVariable.id = newVariableConfig.id
			}
			newMappedVariable.value = newVariableConfig.value
			newMappedVariable.description = newVariableConfig.description
			newMappedVariable.isPassword = newVariableConfig.isPassword
			mapClosure(newMappedVariable, variable, variableId)
		}
	}
	
}
