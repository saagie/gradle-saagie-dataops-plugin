package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExportVariables  implements IExists{
	VariableEnvironmentDTO variableEnvironmentDTO = new VariableEnvironmentDTO();
	
	@Override
	boolean exists() {
		return variableEnvironmentDTO.exists() || variableEnvironmentDTO.exists()
	}
	
}
