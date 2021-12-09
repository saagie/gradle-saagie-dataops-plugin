package io.saagie.plugin.dataops.models.graphPipeline

import io.saagie.plugin.dataops.models.IMapable

class GraphPipelineVersion implements IMapable {
    String releaseNote
    PipelineGraph graph = new PipelineGraph()

    Object graph(Closure closure) {
        graph.with(closure)
    }

    @Override
    Map toMap() {
        return [
            releaseNote : releaseNote,
            graph       : graph.toMap()
        ]
    }
}
