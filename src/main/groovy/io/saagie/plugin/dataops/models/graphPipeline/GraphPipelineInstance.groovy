package io.saagie.plugin.dataops.models.graphPipeline

import io.saagie.plugin.dataops.models.IMapable

class GraphPipelineInstance implements IMapable {
    String id

    @Override
    Map toMap() {
        if (id) return [id: id]
        return null
    }
}
