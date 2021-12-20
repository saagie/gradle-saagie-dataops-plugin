package io.saagie.plugin.dataops.models.graphPipeline

import groovy.transform.TypeChecked
import io.saagie.plugin.dataops.models.IMapable;

@TypeChecked
class JobNode implements IMapable {
    String id
    JobInGraphPipeline job = new JobInGraphPipeline()
    NodePosition position = new NodePosition()
    List<String> nextNodes = []

    Object position(Closure closure) {
        position.with(closure)
    }

    Object job(Closure closure) {
        job.with(closure)
    }

    @Override
    Map toMap() {
        def jobNodeMap = [
            id          : id,
            job         : job.toMap(),
            nextNodes   : nextNodes
        ]

        if (position && position.x && position.y) {
            jobNodeMap.put('position', position.toMap())
        }

        return jobNodeMap
    }
}

@TypeChecked
class JobInGraphPipeline implements IMapable {
    String id

    @Override
    Map toMap() {
        return [
            id : id
        ]
    }
}
