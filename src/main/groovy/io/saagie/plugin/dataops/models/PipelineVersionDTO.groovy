package io.saagie.plugin.dataops.models

class PipelineVersionDTO  implements IExists{
    String releaseNote
    def jobs =[]

    @Override
    boolean exists() {
        return releaseNote ||  ( jobs && jobs.length )
    }

    void setPipelineVersionFromApiResult(pipelineVersionDetailResult) {
        initPipelineVersionWithCommunFields(pipelineVersionDetailResult)
    }

    void setPipelineVersionFromV1ApiResult(jobs, releaseNote) {
        this.releaseNote = releaseNote
        this.jobs = jobs
    }

    def initPipelineVersionWithCommunFields (pipelineVersionDetailResult) {
        releaseNote = pipelineVersionDetailResult.releaseNote
        jobs = pipelineVersionDetailResult.jobs.collect { [id: it.id]}
    }
}
