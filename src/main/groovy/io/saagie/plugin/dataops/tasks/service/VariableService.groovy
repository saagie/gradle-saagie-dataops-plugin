package io.saagie.plugin.dataops.tasks.service

import io.saagie.plugin.dataops.models.ListVariableContainer
import org.codehaus.groovy.runtime.MethodClosure

@Singleton
class VariableService {
	ArrayList<ListVariableContainer> listVariableContaines = []
	
	def getVariableList( variable, projectId, operation ) {
		def found = listVariableContaines.find {
			return it.projectId.equals(projectId) && variable.scope.equals(it.scope) && it.listVariableByNameAndScope?.size() > 0
		}
		if (found) {
			return found.listVariableByNameAndScope
		} else {
			ListVariableContainer listVariableContainer = []
			listVariableContainer.projectId = projectId
			listVariableContainer.scope = variable.scope
			listVariableContainer.listVariableByNameAndScope = operation(variable)
			listVariableContaines.add(listVariableContainer)
			return listVariableContainer.listVariableByNameAndScope
		}
	}
}
