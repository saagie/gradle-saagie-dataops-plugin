package io.saagie.plugin.dataops.models.graphPipeline

import groovy.transform.TypeChecked
import io.saagie.plugin.dataops.models.IMapable

@TypeChecked
class NodePosition implements IMapable {
    Float x
    Float y

    @Override
    Map toMap() {
        return [
            x : x,
            y : y
        ]
    }
}
