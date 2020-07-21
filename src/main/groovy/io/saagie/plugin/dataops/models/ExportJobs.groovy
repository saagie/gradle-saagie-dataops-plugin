package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExportJobs implements IExists{
    JobDTO jobDTO = new JobDTO()
    JobVersionDTO jobVersionDTO  = new JobVersionDTO()
    ArrayList<JobVersionDTO> versions = new ArrayList<JobVersionDTO>()
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

    void setJobVersionFromApiResult(version) {
        jobVersionDTO.setJobVersionFromApiResult(version)
    }

    void setJobFromV1ApiResult(jobV1DetailResult, technology, cronScheduling ) {
        jobDTO.setJobFromV1ApiResult(jobV1DetailResult, technology, cronScheduling)
    }

    void setJobVersionFromV1ApiResult(runTimeVersion, currentVersion, Long filesize){
        jobVersionDTO.setJobVersionFromV1ApiResult(runTimeVersion, currentVersion, filesize)
    }

    void addJobVersionFromV1ApiResult(runTimeVersion, currentVersion, Long fileSize) {
        JobVersionDTO jobVersionDTOElement = new JobVersionDTO()
        jobVersionDTOElement.setJobVersionFromV1ApiResult(runTimeVersion, currentVersion, fileSize)
        versions.add(jobVersionDTOElement)
    }
}
