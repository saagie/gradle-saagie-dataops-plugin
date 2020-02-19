package io.saagie.plugin.dataops.tasks.projects.importtask

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Pipeline
import io.saagie.plugin.dataops.utils.SaagieUtils

class ImportPipelineService {

    def static importAndCreatePipelines(Map pipelines, DataOpsExtension globalConfig, Closure mapClosure) {
            pipelines.each { pipeline ->
                def piplelineId = pipeline.key
                Map pipelineConfigOverride = pipeline.value.configOverride

                def newPipelineConfigWithOverride = [
                    *:pipelineConfigOverride.pipeline,
                    *: SaagieUtils.extractProperties(globalConfig.pipelineOverride),
                    id: piplelineId
                ]

                globalConfig.pipeline = newPipelineConfigWithOverride as Pipeline
                globalConfig.pipelineVersion {
                    releaseNote = pipelineConfigOverride.pipelineVersion.releaseNote
                    jobs = pipelineConfigOverride.pipelineVersion.jobs
                }
                mapClosure(globalConfig, pipeline)
            }

    }
}
