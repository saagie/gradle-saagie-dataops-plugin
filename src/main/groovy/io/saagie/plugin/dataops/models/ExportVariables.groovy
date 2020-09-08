package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked
import io.saagie.plugin.dataops.models.interfaces.VariableEnvironmentDTOInterface

@TypeChecked
class ExportVariables implements IExists {
	VariableEnvironmentDTOInterface variableEnvironmentDTO
	
	@Override
	boolean exists() {
		return variableEnvironmentDTO.exists()
	}
	
}
