package io.saagie.plugin.dataops.models.graphPipeline

import io.saagie.plugin.dataops.models.Pipeline

class GraphPipelineMapper {
    Pipeline pipeline = new Pipeline()
    GraphPipelineVersion graphPipelineVersion = new GraphPipelineVersion()

    Object pipeline(Closure closure) {
        pipeline.with(closure)
    }

    Object graphPipelineVersion(Closure closure) {
        graphPipelineVersion.with(closure)
    }
}
