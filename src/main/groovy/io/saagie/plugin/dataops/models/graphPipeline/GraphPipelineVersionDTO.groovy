package io.saagie.plugin.dataops.models.graphPipeline

import io.saagie.plugin.dataops.models.IExists
import io.saagie.plugin.dataops.utils.SaagieUtils
import org.jetbrains.annotations.NotNull

class GraphPipelineVersionDTO implements IExists, Comparable {
    String releaseNote
    def number
    def graph

    @Override
    boolean exists() {
        return releaseNote || graph
    }

    void setPipelineVersionFromApiResult(pipelineVersionDetailResult) {
        initPipelineVersionWithCommunFields(pipelineVersionDetailResult)
    }

    void setPipelineVersionFromV1ApiResult(graph, String releaseNote, String number) {
        this.releaseNote = releaseNote
        this.graph = graph
        if (number) {
            this.number = number
        }
    }

    def initPipelineVersionWithCommunFields(pipelineVersionDetailResult) {
        releaseNote = pipelineVersionDetailResult.releaseNote
        graph = pipelineVersionDetailResult.graph
        if (pipelineVersionDetailResult.number) {
            this.number = pipelineVersionDetailResult.number
        }
    }

    // TODO 2875: modify with graph instead jobs
    @Override
    int compareTo(@NotNull Object o) {
        def diffJobs = SaagieUtils.getDifferenceOfTwoArrays(jobs, o.jobs)
        return diffJobs.size() > 0 ? 1 : releaseNote <=> o.releaseNote
    }
}
