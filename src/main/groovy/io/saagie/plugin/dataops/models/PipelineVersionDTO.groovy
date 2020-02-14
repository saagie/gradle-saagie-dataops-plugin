package io.saagie.plugin.dataops.models

class PipelineVersionDTO  implements IExists{
    String releaseNote
    def jobs =[]

    @Override
    boolean exists() {
        return releaseNote ||  ( jobs && jobs.length )
    }

    void setPipelineVersionFromApiResult(pipelineVersionDetailResult) {
        releaseNote = pipelineVersionDetailResult.releaseNote
        jobs = pipelineVersionDetailResult.jobs
    }
}
