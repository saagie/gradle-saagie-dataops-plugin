package  io.saagie.plugin.dataops.models.interfaces

import io.saagie.plugin.dataops.models.IExists
import io.saagie.plugin.dataops.models.VariableEnvironmentDetailDTO

interface VariableEnvironmentDTOInterface extends IExists {
	VariableEnvironmentDetailDTO variableDetail = new VariableEnvironmentDetailDTO();
	setVariableDetailValuesFromData(variableEnvData);
}
