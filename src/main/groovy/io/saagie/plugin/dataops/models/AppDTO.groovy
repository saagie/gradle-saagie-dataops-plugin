package io.saagie.plugin.dataops.models

class AppDTO extends JobDTO {
    int storageSizeInMB;
    Boolean isStreaming;

    void setAppFromApiResult(appDetailResult) {
        this.storageSizeInMB = appDetailResult.storageSizeInMB;
        this.isStreaming = appDetailResult.isStreaming;
        this.setJobFromApiResult(appDetailResult);
    }
}
