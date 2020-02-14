package io.saagie.plugin.dataops.models

class PipelineVersionDTO {
    String releaseNote
    def jobs =[]

    @Override
    boolean exists() {
        return releaseNote ||  ( jobs && jobs.length )
    }

    void setPipelineVersionFromApiResult(pipelineVersionDetailResult) {
        releaseNote = pipelineDetailResult.releaseNote
        jobs = pipelineDetailResult.jobs
    }
}
