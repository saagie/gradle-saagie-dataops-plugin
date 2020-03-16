package io.saagie.plugin.dataops.tasks.projects.importtask

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.AlertingDTO
import io.saagie.plugin.dataops.models.Pipeline
import io.saagie.plugin.dataops.models.PipelineOverride
import io.saagie.plugin.dataops.models.PipelineVersion
import io.saagie.plugin.dataops.models.PropertyOverride
import io.saagie.plugin.dataops.models.PipelineMapper
import io.saagie.plugin.dataops.utils.SaagieUtils

class ImportPipelineService {

    def static importAndCreatePipelines(Map pipelines, DataOpsExtension globalConfig, Closure mapClosure, jobList) {
            pipelines.each { pipeline ->
                def piplelineId = pipeline.key
                Map pipelineConfigOverride = pipeline.value.configOverride
                // delete next line
                def pipelineOverride = SaagieUtils.extractProperties(globalConfig.pipelineOverride as PropertyOverride)
                def newPipelineConfigWithOverride = [
                    *:pipelineConfigOverride.pipeline
                ]

                if(globalConfig.pipelineOverride) {
                    newPipelineConfigWithOverride << [ *: [
                        isScheduled : globalConfig.pipelineOverride?.isScheduled,
                        cronScheduling : globalConfig.pipelineOverride?.cronScheduling
                    ]]

                    if(globalConfig.pipelineOverride.alerting && globalConfig.pipelineOverride.alerting.emails){
                        newPipelineConfigWithOverride << [*:[alerting : globalConfig.pipelineOverride?.alerting]]
                    }
                }

                PipelineMapper newMappedPipeLineData = new PipelineMapper()
                newMappedPipeLineData.pipeline {
                     name = newPipelineConfigWithOverride.name
                     description = newPipelineConfigWithOverride.description
                     isScheduled = newPipelineConfigWithOverride.isScheduled
                     cronScheduling = newPipelineConfigWithOverride.cronScheduling
                }

                if(newPipelineConfigWithOverride.alerting?.emails) {
                    newMappedPipeLineData.pipeline.alerting {
                        emails = newPipelineConfigWithOverride.alerting?.emails
                        statusList = newPipelineConfigWithOverride.alerting?.statusList
                        logins = newPipelineConfigWithOverride.alerting?.logins
                    }
                }

                newMappedPipeLineData.pipelineVersion {
                    releaseNote = pipelineConfigOverride.pipelineVersion?.releaseNote
                    jobs = getJobsNameFromJobList(jobList, pipelineConfigOverride.pipelineVersion?.jobs)
                }

                mapClosure(newMappedPipeLineData, pipeline, pipeline.key)
            }
    }

    def static getJobsNameFromJobList(jobs, JobsFromPipelines) {
        def jobForPipeVersionArray = []
        if(jobs && JobsFromPipelines) {
            jobs.each { job ->
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
