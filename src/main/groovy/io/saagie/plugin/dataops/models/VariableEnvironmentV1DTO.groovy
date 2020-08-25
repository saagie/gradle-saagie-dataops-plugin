package io.saagie.plugin.dataops.models

import io.saagie.plugin.dataops.models.interfaces.VariableEnvironmentDTOInterface

class VariableEnvironmentV1DTO implements IExists, VariableEnvironmentDTOInterface {
	VariableEnvironmentDetailDTO variableDetail;
	long platformId
	
	def setVariableDetailValuesFromData(variableEnvV2Data) {
		this.platformId = variableEnvV2Data.platformId
		variableDetail.setVariableDetailValuesFromData(variableEnvV2Data, true)
	}
	
	@Override
	boolean exists() {
		return name && variableDetail.exists()
	}
}
