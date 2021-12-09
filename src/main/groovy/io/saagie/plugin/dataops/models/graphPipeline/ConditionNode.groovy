package io.saagie.plugin.dataops.models.graphPipeline

import groovy.transform.TypeChecked
import io.saagie.plugin.dataops.models.IMapable;

@TypeChecked
class ConditionNode implements IMapable {
    String id
    NodePosition position = new NodePosition()
    List<String> nextNodesSuccess = []
    List<String> nextNodesFailure = []

    Object position(Closure closure) {
        position.with(closure)
    }

    @Override
    Map toMap() {
        def conditionNodeMap = [
            id               : id,
            nextNodesSuccess : nextNodesSuccess,
            nextNodesFailure : nextNodesFailure
        ]
        if (position && position.x && position.y) {
            conditionNodeMap.put('position', position.toMap())
        }

        return conditionNodeMap
    }
}
