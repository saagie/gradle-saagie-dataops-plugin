package io.saagie.plugin.dataops.tasks.projects.importtask

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Pipeline
import io.saagie.plugin.dataops.utils.SaagieUtils

class ImportPipelineService {

    def static importAndCreatePipelines(Map pipelines, DataOpsExtension globalConfig, Closure mapClosure, jobList) {
            pipelines.each { pipeline ->
                def piplelineId = pipeline.key
                Map pipelineConfigOverride = pipeline.value.configOverride

                def newPipelineConfigWithOverride = [
                    *:pipelineConfigOverride.pipeline,
                    *: SaagieUtils.extractProperties(globalConfig.pipelineOverride),
                    id: piplelineId
                ]

                globalConfig.pipeline {
                     id = newPipelineConfigWithOverride.id
                     name = newPipelineConfigWithOverride.name
                     description = newPipelineConfigWithOverride.description
                     ids = newPipelineConfigWithOverride.ids
                     include_job = newPipelineConfigWithOverride.include_job
                     isScheduled = newPipelineConfigWithOverride.isScheduled
                     cronScheduling = newPipelineConfigWithOverride.cronScheduling
                     alerting {
                         emails = newPipelineConfigWithOverride.alerting?.emails
                         logins = newPipelineConfigWithOverride.alerting?.logins
                         statusList = newPipelineConfigWithOverride.alerting?.statusList
                     }
                }

                globalConfig.pipelineVersion {
                    releaseNote = pipelineConfigOverride.pipelineVersion?.releaseNote
                    jobs = getJobsNameFromJobList(jobList, pipelineConfigOverride.pipelineVersion?.jobs)
                }
                mapClosure(globalConfig, pipeline)
            }
    }

    def static getJobsNameFromJobList(jobs, JobsFromPipelines) {
        def jobForPipeVersionArray = null
        if(jobs && JobsFromPipelines) {
            jobForPipeVersionArray = jobs.each { job ->
                JobsFromPipelines.each { jobPipeline ->
                    if(jobPipeline.name == job.name){
                        jobForPipeVersionArray.add(job.id)
                    }
                }
            }
        }
        return jobForPipeVersionArray
    }

}
