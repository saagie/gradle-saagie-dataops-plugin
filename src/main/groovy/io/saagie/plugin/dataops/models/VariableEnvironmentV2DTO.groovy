package io.saagie.plugin.dataops.models

import io.saagie.plugin.dataops.models.interfaces.VariableEnvironmentDTOInterface;

class VariableEnvironmentV2DTO implements VariableEnvironmentDTOInterface {
    String name
    VariableEnvironmentDetailDTO variableDetail = new VariableEnvironmentDetailDTO()
    ArrayList<VariableEnvironmentDetailDTO> overridenValues = new ArrayList<VariableEnvironmentDetailDTO>()

    void setVariableDetailValuesFromData(variableEnvV2Data) {
        this.name = variableEnvV2Data.name
        variableDetail.setVariableDetailValuesFromData(variableEnvV2Data, false)
        if (variableEnvV2Data.overriddenValues && variableEnvV2Data.overriddenValues.size() > 0) {
            setOverridenValues(variableEnvV2Data.overriddenValues)
        }
    }

    def setOverridenValues(ArrayList overridenValues) {
        overridenValues.forEach {
            VariableEnvironmentDetailDTO variableDetailElement = []
            variableDetailElement.setVariableDetailValuesFromData(it, false)
            this.overridenValues.add(variableDetailElement)
        }
    }

    @Override
    boolean exists() {
        return name && variableDetail.exists()
    }
}
