package io.saagie.plugin.dataops.models

import io.saagie.plugin.dataops.models.interfaces.VariableEnvironmentDTOInterface

class VariableEnvironmentV1DTO implements IExists, VariableEnvironmentDTOInterface {
    String name
    VariableEnvironmentDetailDTO variableDetail = new VariableEnvironmentDetailDTO()
    long platformId

    void setVariableDetailValuesFromData(variableEnvV2Data) {
        this.platformId = variableEnvV2Data.platformId
        this.name = variableEnvV2Data.name
        variableDetail.setVariableDetailValuesFromData(variableEnvV2Data, true)
    }

    @Override
    boolean exists() {
        return name && variableDetail.exists()
    }
}
