package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExportJob implements IExists{
    JobDTO jobDTO = new JobDTO()
    JobVersionDTO jobVersion  = new JobVersionDTO()
    String downloadUrl

    @Override
    boolean exists() {
        return jobDTO.exists() && jobVersion && downloadUrl
    }
    void setJobFromApiResult(jobDetailResult) {
        jobDTO.setJobFromApiResult(jobDetailResult)
    }
}
