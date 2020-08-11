package io.saagie.plugin.dataops.models

class VariableEnvironmentDetailDTO implements IExists {
	String scope
	String id
	String value
	String description
	boolean isPassword
	
	def setVariableDetailValuesFromData( variableEnvV2Data ) {
		
		scope = variableEnvV2Data.scope
		
		if (variableEnvV2Data?.id) {
			id = variableEnvV2Data.id
		}
		
		if (variableEnvV2Data?.description) {
			description = variableEnvV2Data.description
		}
		
		if (variableEnvV2Data?.value) {
			value = variableEnvV2Data.value
		}
		
		if (variableEnvV2Data?.isPassword) {
			isPassword = variableEnvV2Data.isPassword
		}
	}
	
	@Override
	boolean exists() {
		return scope && ( id || description || value)
	}
}
