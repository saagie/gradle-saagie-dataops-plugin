package io.saagie.plugin.dataops.models.graphPipeline

class GraphPipelineMapper {
    GraphPipeline pipeline = new GraphPipeline()
    GraphPipelineVersion pipelineVersion = new GraphPipelineVersion()

    Object pipeline(Closure closure) {
        pipeline.with(closure)
    }

    Object pipelineVersion(Closure closure) {
        pipelineVersion.with(closure)
    }
}
