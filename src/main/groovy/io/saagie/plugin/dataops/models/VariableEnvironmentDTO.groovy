package io.saagie.plugin.dataops.models

class VariableEnvironmentDTO implements IExists{
	String name
	VariableEnvironmentDetailDTO variableDetail = new VariableEnvironmentDetailDTO()
	ArrayList<VariableEnvironmentDetailDTO> overridenValues = new ArrayList<VariableEnvironmentDetailDTO>()
	
	def setVariableDetailValuesFromData(variableEnvV2Data) {
		this.name = variableEnvV2Data.name
		variableDetail.setVariableDetailValuesFromData(variableEnvV2Data)
		if( variableEnvV2Data.overriddenValues && variableEnvV2Data.overriddenValues.size() > 0 ) {
			setOverridenValues(variableEnvV2Data.overriddenValues)
		}
	}
	
	def setOverridenValues( ArrayList overridenValues) {
		overridenValues.forEach {
			VariableEnvironmentDetailDTO variableDetailElement = []
			variableDetailElement.setVariableDetailValuesFromData(it)
			this.overridenValues.add(variableDetailElement)
		}
		
		
	}
	
	@Override
	boolean exists() {
		return name && variableDetail.exists()
	}
}
