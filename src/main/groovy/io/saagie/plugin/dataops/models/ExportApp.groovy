package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExportApp implements IExists {
    AppDTO appDTO = new AppDTO()
    AppVersionDTO appVersionDTO = new AppVersionDTO()
    ArrayList<AppVersionDTO> versions = new ArrayList<AppVersionDTO>()

    @Override
    boolean exists() {
        return appDTO.exists() && appVersionDTO.exists()
    }

    void setAppFromApiResult(appDetailResult) {
        appDTO.setAppFromApiResult(appDetailResult)
    }

    void setAppVersionFromApiResult(version) {
        appVersionDTO.setAppVersionFromApiResult(version)
    }


    void addAppVersionFromV2ApiResult(version) {
        AppVersionDTO appVersionDTOElement = new AppVersionDTO()
        appVersionDTOElement.setAppVersionFromApiResult(version)
        versions.add(appVersionDTOElement)
    }

}
