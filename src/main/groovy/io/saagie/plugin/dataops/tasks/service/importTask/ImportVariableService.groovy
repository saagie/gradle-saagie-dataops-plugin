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
					* : variableValue.variable
			]
			
			VariableMapper newMappedVariable = new VariableMapper()
			newMappedVariable.name = newVariableConfig.name
			newMappedVariable.scope = newVariableConfig.scope
			newMappedVariable.id = newVariableConfig.id
			newMappedVariable.value = newVariableConfig.value
			newMappedVariable.description = newVariableConfig.description
			newMappedVariable.isPassword = newVariableConfig.isPassword
			mapClosure(newMappedVariable, variable, variableId)
		}
	}
	
}
