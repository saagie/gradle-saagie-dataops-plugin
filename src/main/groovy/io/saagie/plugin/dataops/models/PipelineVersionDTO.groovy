package io.saagie.plugin.dataops.models

import io.saagie.plugin.dataops.models.graphPipeline.ConditionNode
import io.saagie.plugin.dataops.models.graphPipeline.JobNode
import io.saagie.plugin.dataops.models.graphPipeline.PipelineGraphDto
import io.saagie.plugin.dataops.utils.SaagieUtils
import org.jetbrains.annotations.NotNull

class PipelineVersionDTO implements IExists, Comparable {
    String releaseNote
    def number
    // Deprecated since graph pipelines
    @Deprecated
    def jobs = []
    PipelineGraphDto graph = new PipelineGraphDto()

    @Override
    boolean exists() {
        return releaseNote || (jobs && jobs.length) || (graph && !graph.jobNodes.isEmpty())
    }

    // V1
    void setPipelineVersionFromV1ApiResult(jobs, String releaseNote, String number) {
        this.releaseNote = releaseNote
        this.jobs = jobs
        if (number) {
            this.number = number
        }
    }

    void setPipelineVersionFromApiResult(pipelineVersionDetailResult) {
        initPipelineVersionWithCommunFields(pipelineVersionDetailResult)
    }

    def initPipelineVersionWithCommunFields(pipelineVersionDetailResult) {
        releaseNote = pipelineVersionDetailResult.releaseNote
        if (pipelineVersionDetailResult.graph) {
            graph = pipelineVersionDetailResult.graph
        } else {
            jobs = pipelineVersionDetailResult.jobs.collect { [id: it.id] }
        }
        if (pipelineVersionDetailResult.number) {
            this.number = pipelineVersionDetailResult.number
        }
    }

    @Override
    int compareTo(@NotNull Object o) {
        if (!graph?.jobNodes?.isEmpty()) {
            return graphCompareTo(o)
        }

        def diffJobs = SaagieUtils.getDifferenceOfTwoArrays(jobs, o.jobs)
        return diffJobs.size() > 0 ? 1 : releaseNote <=> o.releaseNote
    }

    private int graphCompareTo(@NotNull Object o) {
        if (releaseNote != o.releaseNote) {
            return releaseNote <=> o.releaseNote
        }

        def jobNodes = graph.jobNodes as List<JobNode>
        def jobNodesParam = o.graph.jobNodes as List<JobNode>
        def conditionNodes = graph.conditionNodes as List<ConditionNode>
        def conditionNodesParam = o.graph.conditionNodes as List<ConditionNode>

        if (!jobNodesListAreEquals(jobNodes, jobNodesParam)
            || !conditionNodesListAreEquals(conditionNodes, conditionNodesParam)
        ) {
            return 1
        }
        return 0
    }

    private static boolean jobNodesListAreEquals(List<JobNode> jobNodes, List<JobNode> jobNodesParam) {
        if (jobNodes.size() != jobNodesParam.size()) {
            return false
        }
        def jobNodesParamSort = jobNodesParam.sort { it.id }
        jobNodes.sort { it.id }.eachWithIndex {jobNode, index ->
            if (jobNode.id != jobNodesParamSort[index].id
                || jobNode.position.x != jobNodesParamSort[index].position.x
                || jobNode.position.y != jobNodesParamSort[index].position.y
                || SaagieUtils.getDifferenceOfTwoArrays(jobNode.nextNodes as ArrayList, jobNodesParamSort[index].nextNodes as ArrayList) > 0
            ) {
                return false
            }
        }
        return true
    }

    private static boolean conditionNodesListAreEquals(List<ConditionNode> conditionNodes, List<ConditionNode> conditionNodesParam) {
        if (conditionNodes.size() != conditionNodesParam.size()) {
            return false
        }
        def conditionNodesParamSort = conditionNodesParam.sort { it.id }
        conditionNodes.sort { it.id }.eachWithIndex {conditionNode, index ->
            if (conditionNode.id != conditionNodesParamSort[index].id
                || conditionNode.job.id != conditionNodesParamSort[index].job.id
                || conditionNode.position.x != conditionNodesParamSort[index].position.x
                || conditionNode.position.y != conditionNodesParamSort[index].position.y
                || SaagieUtils.getDifferenceOfTwoArrays(conditionNode.nextNodesSuccess as ArrayList, conditionNodesParamSort[index].nextNodesSuccess as ArrayList) > 0
                || SaagieUtils.getDifferenceOfTwoArrays(conditionNode.nextNodesFailure as ArrayList, conditionNodesParamSort[index].nextNodesFailure as ArrayList) > 0
            ) {
                return false
            }
        }
        return true
    }
}
