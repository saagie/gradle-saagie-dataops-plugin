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

    void setPipelineFromApiResult(jobDetailResult) {
        pipelineDTO.setPipelineFromApiResult(jobDetailResult)
    }

    void setPipelineVersionFromApiResult(version){
        pipelineVersionDTO.setPipelineVersionFromApiResult(version)
    }
}
