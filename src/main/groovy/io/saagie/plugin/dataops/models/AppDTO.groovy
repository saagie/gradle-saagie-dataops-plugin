package io.saagie.plugin.dataops.models

class AppDTO extends JobDTO {
    int storageSizeInMB;

    void setAppFromApiResult(appDetailResult) {
        this.storageSizeInMB = appDetailResult.storageSizeInMB;
        this.setJobFromApiResult(appDetailResult);
    }
}
