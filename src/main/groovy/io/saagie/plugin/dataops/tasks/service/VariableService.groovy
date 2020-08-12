package io.saagie.plugin.dataops.tasks.service

import io.saagie.plugin.dataops.models.VariableListContainer

@Singleton
class VariableService {
	ArrayList<VariableListContainer> listVariableContaines = []
	
	def getVariableList( variable, projectId, operation ) {
		def foundVariable = listVariableContaines.find {
			return it.projectId.equals(projectId) && variable.scope.equals(it.scope) && it.variableListByNameAndScope?.size() > 0
		}
		if (foundVariable) {
			return foundVariable.variableListByNameAndScope
		} else {
			VariableListContainer listVariableContainer = []
			listVariableContainer.projectId = projectId
			listVariableContainer.scope = variable.scope
			listVariableContainer.variableListByNameAndScope = operation(variable)
			listVariableContaines.add(listVariableContainer)
			return listVariableContainer.variableListByNameAndScope
		}
	}
}
