package io.saagie.plugin.dataops.tasks.service.importTask

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.PipelineVersion
import io.saagie.plugin.dataops.models.ArtifactPropertyOverride
import io.saagie.plugin.dataops.models.PipelineMapper
import io.saagie.plugin.dataops.utils.SaagieUtils
import org.apache.groovy.json.internal.LazyMap
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class ImportPipelineService {

    static final Logger logger = Logging.getLogger(ImportPipelineService.class)

    def static importAndCreatePipelines(Map pipelines, DataOpsExtension globalConfig, Closure mapClosure, jobList, hasGraphPipelines) {
        pipelines.each { pipeline ->
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

            if (hasGraphPipelines) {
                // Checking that all jobs in the graph pipeline, exists in project and use them in graph
                newMappedPipeLineData.pipelineVersion {
                    releaseNote = pipelineConfigOverride.pipelineVersion?.releaseNote
                    graph = getJobNodesFromJobsExisting(jobList, pipelineConfigOverride.pipelineVersion?.graph)
                }
            } else {
                newMappedPipeLineData.pipelineVersion {
                    releaseNote = pipelineConfigOverride.pipelineVersion?.releaseNote
                    jobs = getJobsIdFromJobsExisting(jobList, pipelineConfigOverride.pipelineVersion?.jobs).collect { it.id }.reverse()
                }
            }

            mapClosure(newMappedPipeLineData, pipeline, pipeline.key, versions, jobList)
        }
    }

    static convertFromMapToJsonVersion(pipelineVersionMap, jobList, hasGraphPipelines) {
        PipelineVersion pipelineVersion = []

        if (hasGraphPipelines) {
            // Checking that all jobs in the graph pipeline, exists in project and use them in graph
            pipelineVersion.with {
                graph = getJobNodesFromJobsExisting(jobList, pipelineVersionMap?.graph)
            }
        } else {
            pipelineVersion.with {
                jobs = getJobsIdFromJobsExisting(jobList, pipelineVersionMap.jobs).collect { it.id }
            }
        }

        if (pipelineVersionMap.releaseNote) {
            pipelineVersion.releaseNote = pipelineVersionMap.releaseNote
        }

        return pipelineVersion
    }

    private def static getJobsIdFromJobsExisting(jobsExistingInProject, jobsFromPipeline) {
        def jobForPipeVersionArray = []
        def jobsNotFound = []
        if (jobsExistingInProject && jobsFromPipeline) {
            jobsFromPipeline.each { jobInPipeline ->
                def existingJobById = jobsExistingInProject.find { (it.id == jobInPipeline.id) }
                def existingJob = existingJobById ?: jobsExistingInProject.find { (it.name == jobInPipeline.name) }
                if (existingJob && existingJob.id) {
                    jobForPipeVersionArray.add existingJob
                } else {
                    jobsNotFound.add jobInPipeline.name
                }
            }
        }
        if (jobsNotFound.size() > 0) {
            logger.error("Some of the jobs contained in the pipeline version doesn't exist in targeted platform.")
            throw new GradleException("Missing job names not found on the target platform => : ${jobsNotFound.toString()}")
        }

        return jobForPipeVersionArray
    }

    private def static getJobNodesFromJobsExisting(jobsExistingInProject, graph) {
        def graphUpdated = graph

        if (jobsExistingInProject && graph?.jobNodes) {
            def currentJobNodes = []
            graph?.jobNodes?.each { jobNode ->

                def existingJobById = jobsExistingInProject.find { (it.id == jobNode.job.id) }
                def existingJob = existingJobById ?: jobsExistingInProject.find { (it.name == jobNode.job.name) }

                if (existingJob && existingJob.id) {
                    def jobNodeUpdated = jobNode
                    jobNodeUpdated.job = [
                        'id': existingJob.id
                    ]
                    currentJobNodes.add(jobNodeUpdated)
                } else {
                    logger.error("Some of the jobs contained in the graph pipeline version doesn't exist in targeted platform in this project.")
                    throw new GradleException("Missing job names not found on the target platform in this project => : ${jobNode.job.name}")
                }
            }
            graphUpdated.jobNodes = currentJobNodes
        }
        return graphUpdated
    }
}
