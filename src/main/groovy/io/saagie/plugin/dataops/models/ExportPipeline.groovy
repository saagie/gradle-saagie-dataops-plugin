package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExportPipeline implements IExists {

    PipelineDTO pipelineDTO = new PipelineDTO()

    PipelineVersionDTO pipelineVersionDTO = new PipelineVersionDTO()
    ArrayList<PipelineVersionDTO> versions = new ArrayList<PipelineVersionDTO>()

    @Override
    boolean exists() {
        return pipelineDTO.exists() || pipelineVersionDTO.exists()
    }

    // V1
    void setPipelineFromV1ApiResult(pipelineDetailResult) {
        pipelineDTO.setPipelineFromV1ApiResult(pipelineDetailResult)
    }
    // V1
    void setPipelineVersionFromV1ApiResult(jobs, String releaseNote, String number = null) {
        pipelineVersionDTO.setPipelineVersionFromV1ApiResult(jobs, releaseNote, number)
    }
    // V1
    void addPipelineVersionDtoToVersionsFromV1(jobs, String releaseNote, String number = null) {
        PipelineVersionDTO pipelineVersionDTOFromVersionsV1 = new PipelineVersionDTO()
        pipelineVersionDTOFromVersionsV1.setPipelineVersionFromV1ApiResult(jobs, releaseNote, number)
        versions.add(0, pipelineVersionDTOFromVersionsV1)
    }

    void setPipelineFromApiResult(pipelineDetailResult) {
        pipelineDTO.setPipelineFromApiResult(pipelineDetailResult)
    }

    void setPipelineVersionFromApiResult(version) {
        pipelineVersionDTO.setPipelineVersionFromApiResult(version)
    }

    void addPipelineVersionFromV2ApiResult(version) {
        PipelineVersionDTO pipelineVersionDTOFromVersionsV2 = new PipelineVersionDTO()
        pipelineVersionDTOFromVersionsV2.setPipelineVersionFromApiResult(version)
        versions.add(0, pipelineVersionDTOFromVersionsV2)
    }
}
