package io.saagie.plugin.dataops.tasks.service.importTask

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.PipelineVersion
import io.saagie.plugin.dataops.models.ArtifactPropertyOverride
import io.saagie.plugin.dataops.models.PipelineMapper
import io.saagie.plugin.dataops.utils.SaagieUtils
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class ImportPipelineService {

    static final Logger logger = Logging.getLogger(ImportPipelineService.class)

    def static importAndCreatePipelines(Map pipelines, DataOpsExtension globalConfig, Closure mapClosure, jobList) {
        pipelines.each { pipeline ->
            def piplelineId = pipeline.key
            Map pipelineConfigOverride = pipeline.value.configOverride
            // delete next line
            def versions = null
            def pipelineOverride = SaagieUtils.extractProperties(globalConfig.pipelineOverride as ArtifactPropertyOverride)
            def newPipelineConfigWithOverride = [
                *: pipelineConfigOverride.pipeline
            ]

            if (globalConfig.pipelineOverride) {
                newPipelineConfigWithOverride << [*: [
                    isScheduled   : globalConfig.pipelineOverride?.isScheduled,
                    cronScheduling: globalConfig.pipelineOverride?.cronScheduling
                ]]

                if (globalConfig.pipelineOverride.alerting && globalConfig.pipelineOverride.alerting.emails) {
                    newPipelineConfigWithOverride << [*: [
                        alerting: globalConfig.pipelineOverride?.alerting
                    ]]
                }
            }

            PipelineMapper newMappedPipeLineData = new PipelineMapper()

            newMappedPipeLineData.pipeline {
                name = newPipelineConfigWithOverride.name
                description = newPipelineConfigWithOverride.description
                isScheduled = newPipelineConfigWithOverride.isScheduled
                cronScheduling = newPipelineConfigWithOverride.cronScheduling
            }

            if (newPipelineConfigWithOverride.alerting?.emails) {
                newMappedPipeLineData.pipeline.alerting {
                    emails = newPipelineConfigWithOverride.alerting?.emails
                    statusList = newPipelineConfigWithOverride.alerting?.statusList
                    logins = newPipelineConfigWithOverride.alerting?.logins
                }
            }

            if (pipelineConfigOverride.versions && pipelineConfigOverride.versions.size() > 0) {
                versions = pipelineConfigOverride.versions
            }

            newMappedPipeLineData.pipelineVersion {
                releaseNote = pipelineConfigOverride.pipelineVersion?.releaseNote
                jobs = getJobsNameFromJobList(jobList, pipelineConfigOverride.pipelineVersion?.jobs).reverse()
            }

            mapClosure(newMappedPipeLineData, pipeline, pipeline.key, versions, jobList)
        }
    }

    def static getJobsNameFromJobList(jobs, JobsFromPipelines) {

        def jobForPipeVersionArray = []
        def jobsNotFound = []
        if (jobs && JobsFromPipelines) {
            def jobsByNames = JobsFromPipelines.name
            jobsByNames.each { job ->
                def existingJob = jobs.find { it.name.equals(job) }
                if (existingJob && existingJob.id) {
                    jobForPipeVersionArray.add existingJob.id
                } else {
                    jobsNotFound.add job
                }
            }
        }
        if (jobsNotFound.size() > 0) {
            logger.error("Some of the jobs contained in the pipeline version doesn't exist in targeted platform.")
            throw new GradleException("Missing job names not found on the target platform => : ${jobsNotFound.toString()}")
        }

        return jobForPipeVersionArray
    }

    static convertFromMapToJsonVersion(pipelineVersionMap, jobList) {
        PipelineVersion pipelineVersion = []

        pipelineVersion.with {
            jobs = getJobsNameFromJobList(jobList, pipelineVersionMap.jobs)
        }

        if (pipelineVersionMap.releaseNote) {
            pipelineVersion.releaseNote = pipelineVersionMap.releaseNote
        }

        return pipelineVersion

    }

}
