package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExportPipeline implements IExists{

    PipelineDTO pipelineDTO = new PipelineDTO()
    PipelineVersionDTO pipelineVersionDTO  = new PipelineVersionDTO()
    ArrayList<PipelineVersionDTO> versions = new ArrayList<PipelineVersionDTO>()
    @Override
    boolean exists() {
        return pipelineDTO.exists() || pipelineVersionDTO.exists()
    }

    void setPipelineFromApiResult(pipelineDetailResult) {
        pipelineDTO.setPipelineFromApiResult(pipelineDetailResult)
    }

    void setPipelineFromV1ApiResult(pipelineDetailResult) {
        pipelineDTO.setPipelineFromV1ApiResult(pipelineDetailResult)
    }

    void setPipelineVersionFromV1ApiResult(jobs, releaseNote) {
        pipelineVersionDTO.setPipelineVersionFromV1ApiResult(jobs, releaseNote)
    }

    void setPipelineVersionFromApiResult(version){
        pipelineVersionDTO.setPipelineVersionFromApiResult(version)
    }

    void addPipelineVersionDTOtoVersions(jobs, releaseNote) {
        PipelineVersionDTO pipelineVersionDTO = new PipelineVersionDTO()
        pipelineVersionDTO.setPipelineVersionFromV1ApiResult(jobs, releaseNote)
        versions.add(pipelineVersionDTO)
    }
}
