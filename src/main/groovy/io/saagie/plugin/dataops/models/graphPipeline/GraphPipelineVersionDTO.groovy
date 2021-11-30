package io.saagie.plugin.dataops.models.graphPipeline

import io.saagie.plugin.dataops.models.IExists
import io.saagie.plugin.dataops.utils.SaagieUtils
import org.jetbrains.annotations.NotNull

class GraphPipelineVersionDTO implements IExists, Comparable {
    String releaseNote
    def number
    def jobs = []

    @Override
    boolean exists() {
        return releaseNote || (jobs && jobs.length)
    }

    void setPipelineVersionFromApiResult(pipelineVersionDetailResult) {
        initPipelineVersionWithCommunFields(pipelineVersionDetailResult)
    }

    void setPipelineVersionFromV1ApiResult(jobs, String releaseNote, String number) {
        this.releaseNote = releaseNote
        this.jobs = jobs
        if (number) {
            this.number = number
        }
    }

    def initPipelineVersionWithCommunFields(pipelineVersionDetailResult) {
        releaseNote = pipelineVersionDetailResult.releaseNote
        jobs = pipelineVersionDetailResult.jobs.collect { [id: it.id] }
        if (pipelineVersionDetailResult.number) {
            this.number = pipelineVersionDetailResult.number
        }
    }

    @Override
    int compareTo(@NotNull Object o) {
        def diffJobs = SaagieUtils.getDifferenceOfTwoArrays(jobs, o.jobs)
        return diffJobs.size() > 0 ? 1 : releaseNote <=> o.releaseNote
    }
}
