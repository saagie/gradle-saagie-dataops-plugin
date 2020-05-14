package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExportPipeline implements IExists{

    PipelineDTO pipelineDTO = new PipelineDTO()
    PipelineVersionDTO pipelineVersionDTO  = new PipelineVersionDTO()

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

    void setPipelineVersionFromV1ApiResult(version) {
        pipelineVersionDTO.setPipelineVersionFromV1ApiResult(version)
    }

    void setPipelineVersionFromApiResult(version){
        pipelineVersionDTO.setPipelineVersionFromApiResult(version)
    }
}
