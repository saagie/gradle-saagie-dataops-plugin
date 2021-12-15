package io.saagie.plugin.dataops.models

import io.saagie.plugin.dataops.models.graphPipeline.PipelineGraph

class PipelineVersion implements IMapable {
    String releaseNote
    // Deprecated since graph pipelines
    @Deprecated
    List<String> jobs = []
    PipelineGraph graph = new PipelineGraph()

    Object graph(Closure closure) {
        graph.with(closure)
    }

    @Override
    Map toMap() {
        if (graph.jobNodes.size() > 0) {
            return [
                releaseNote : releaseNote,
                graph       : graph.toMap()
            ]
        }
        if (!jobs.empty) {
            return [
                releaseNote: releaseNote,
                jobs       : jobs
            ]
        }
        return null
    }
}
