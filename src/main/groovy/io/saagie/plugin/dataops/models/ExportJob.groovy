package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExportJob implements IExists{
    JobDTO jobDTO = new JobDTO()
    JobVersionDTO jobVersionDTO  = new JobVersionDTO()
    String downloadUrl
    int downloadUrlVersion

    @Override
    boolean exists() {
        return jobDTO.exists() && jobVersionDTO.exists()
    }

    void setJobFromApiResult(jobDetailResult) {
        jobDTO.setJobFromApiResult(jobDetailResult)
    }

    void setJobVersionFromApiResult(version){
        jobVersionDTO.setJobVersionFromApiResult(version)
    }

    void setJobFromV1ApiResult(jobDetailResult) {
        jobDTO.setJobFromV1ApiResult(jobDetailResult)
    }

    void setJobVersionFromV1ApiResult(version){
        jobVersionDTO.setJobVersionFromV1ApiResult(version)
    }
}
