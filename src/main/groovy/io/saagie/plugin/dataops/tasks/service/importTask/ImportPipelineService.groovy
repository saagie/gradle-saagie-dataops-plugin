package io.saagie.plugin.dataops.tasks.service.importTask

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.PipelineVersion
import io.saagie.plugin.dataops.models.PropertyOverride
import io.saagie.plugin.dataops.models.PipelineMapper
import io.saagie.plugin.dataops.utils.SaagieUtils

class ImportPipelineService {
	
	def static importAndCreatePipelines( Map pipelines, DataOpsExtension globalConfig, Closure mapClosure, jobList ) {
		pipelines.each { pipeline ->
			def piplelineId = pipeline.key
			Map pipelineConfigOverride = pipeline.value.configOverride
			// delete next line
			def versions = null
			def pipelineOverride = SaagieUtils.extractProperties(globalConfig.pipelineOverride as PropertyOverride)
			def newPipelineConfigWithOverride = [
					* : pipelineConfigOverride.pipeline
			]
			
			if (globalConfig.pipelineOverride) {
				newPipelineConfigWithOverride << [* : [
						isScheduled    : globalConfig.pipelineOverride?.isScheduled,
						cronScheduling : globalConfig.pipelineOverride?.cronScheduling
				]]
				
				if (globalConfig.pipelineOverride.alerting && globalConfig.pipelineOverride.alerting.emails) {
					newPipelineConfigWithOverride << [* : [alerting : globalConfig.pipelineOverride?.alerting]]
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
	
	def static getJobsNameFromJobList( jobs, JobsFromPipelines ) {
		
		def jobForPipeVersionArray = []
		
		if (jobs && JobsFromPipelines) {
			def jobsByNames = JobsFromPipelines.name
			jobs.each { job ->
				def existingJob = jobsByNames.find { it.equals(job.name)}
				if(existingJob) {
					jobForPipeVersionArray.add(job.id)
				}
			}
		}
		
		return jobForPipeVersionArray
	}
	
	static convertFromMapToJsonVersion( pipelineVersionMap, jobList ) {
		PipelineVersion pipelineVersion = []
		
		pipelineVersion.with {
			jobs = getJobsNameFromJobList(jobList, pipelineVersionMap.jobs)
		}
		
		if (pipelineVersion.releaseNote) {
			pipelineVersion {
				releaseNote = pipelineVersion.releaseNote
			}
		}
		
		return pipelineVersion
		
	}
	
}
