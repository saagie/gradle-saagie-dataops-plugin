package  io.saagie.plugin.dataops.models.interfaces

import io.saagie.plugin.dataops.models.IExists
import io.saagie.plugin.dataops.models.VariableEnvironmentDetailDTO

interface VariableEnvironmentDTOInterface extends IExists {
	String name
	VariableEnvironmentDetailDTO variableDetail = new VariableEnvironmentDetailDTO()
	void setVariableDetailValuesFromData(variableEnvData)
}
