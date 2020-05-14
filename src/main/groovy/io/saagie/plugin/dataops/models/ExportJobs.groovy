package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExportJobs implements IExists{
    JobDTO jobDTO = new JobDTO()
    JobVersionDTO jobVersionDTO  = new JobVersionDTO()
    String downloadUrl
    int downloadUrlVersion
    boolean isV1 = false

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

    void setJobFromV1ApiResult(jobDetailResult, technology, technologyVersion, cronScheduling ) {
        jobDTO.setJobFromV1ApiResult(jobDetailResult, technology, technologyVersion, cronScheduling)
    }

    void setJobVersionFromV1ApiResult(version, runTimeVersion){
        jobVersionDTO.setJobVersionFromV1ApiResult(version)
    }
}
