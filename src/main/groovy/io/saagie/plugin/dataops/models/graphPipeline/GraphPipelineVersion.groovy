package io.saagie.plugin.dataops.models.graphPipeline

import io.saagie.plugin.dataops.models.IMapable

class GraphPipelineVersion implements IMapable {
    String releaseNote
    List<String> jobs = []

    @Override
    Map toMap() {
        if (releaseNote && !jobs.empty) {
            return [
                releaseNote: releaseNote,
                jobs       : jobs
            ]
        }
        return null
    }
}
