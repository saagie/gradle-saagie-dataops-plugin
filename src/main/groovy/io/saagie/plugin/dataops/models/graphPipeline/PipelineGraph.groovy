package io.saagie.plugin.dataops.models.graphPipeline

import io.saagie.plugin.dataops.models.IMapable

class PipelineGraph implements IMapable {

    List<Closure<JobNode>> jobNodes = []
    List<Closure<ConditionNode>> conditionNodes = []

    Object jobNodes(Closure closure) {
        jobNodes.with(closure)
    }

    Object conditionNodes(Closure closure) {
        conditionNodes.with(closure)
    }

    @Override
    Map toMap() {
        return [
            jobNodes         : jobNodes.collect {
                JobNode jobNode = new JobNode()
                jobNode.with(it)
                return jobNode.toMap()
            },
            conditionNodes   : conditionNodes.collect {
                ConditionNode conditionNode = new ConditionNode()
                conditionNode.with(it)
                return conditionNode.toMap()
            },
        ]
    }
}
