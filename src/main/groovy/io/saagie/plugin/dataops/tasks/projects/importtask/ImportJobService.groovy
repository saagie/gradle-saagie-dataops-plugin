package io.saagie.plugin.dataops.tasks.projects.importtask

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Alerting
import io.saagie.plugin.dataops.models.Job
import io.saagie.plugin.dataops.models.JobMapper
import io.saagie.plugin.dataops.models.JobVersion
import io.saagie.plugin.dataops.utils.SaagieUtils

class ImportJobService {

    def static parsePipelines(File extractedFile, String createdJobs) {
        // read all params inside the file

        // for each pipeline, map them with the right
    }

    def static importAndCreateJobs(Map jobs, DataOpsExtension globalConfig, Closure mapClosure) {
        // First, map over all jobs
        // For each job, create it with the rules below:
        //  - the id of the job is not imported.
        //  - if a job with the same name already exists, it's creating a new version of the job.
        //  - if no job exists with the same name, it's creating a new job.
        jobs.each { job ->
            def jobId = job.key
            Map jobConfigOverride = job.value.configOverride
            File jobPackageFile = job.value.package

            def newJobConfigWithOverride = [
                * : jobConfigOverride.job
            ]

            if(globalConfig.jobOverride) {
                newJobConfigWithOverride << [ *: [
                    isScheduled : globalConfig.jobOverride?.isScheduled,
                    cronScheduling : globalConfig.jobOverride?.cronScheduling,
                ]]

                if(globalConfig.jobOverride.alerting?.emails){
                    newJobConfigWithOverride << [*:[alerting : globalConfig.jobOverride?.alerting]]
                }
            }


            JobMapper newMappedJobData = new JobMapper()

            newMappedJobData.job {
                name = newJobConfigWithOverride.name
                technology = newJobConfigWithOverride.technology
                cronScheduling = newJobConfigWithOverride.cronScheduling
                isScheduled =  newJobConfigWithOverride.isScheduled
                category = newJobConfigWithOverride.category
                description = newJobConfigWithOverride.description
            }

            if(newJobConfigWithOverride.alerting?.emails) {
                newMappedJobData.job.alerting {
                    emails = newJobConfigWithOverride.alerting?.emails
                    statusList = newJobConfigWithOverride.alerting?.statusList
                    logins = newJobConfigWithOverride.alerting?.logins
                }
            }

            newMappedJobData.jobVersion {
                commandLine = jobConfigOverride.jobVersion.commandLine
                releaseNote = jobConfigOverride.jobVersion.releaseNote
                runtimeVersion = jobConfigOverride.jobVersion.runtimeVersion
                dockerInfo {
                    image = jobConfigOverride.jobVersion.dockerInfo?.image
                    dockerCredentialsId = jobConfigOverride.jobVersion.dockerInfo?.dockerCredentialsId
                }
            }

            if(newJobConfigWithOverride.jobVersion?.packageInfo) {
                newMappedJobData.jobVersion?.packageInfo {
                    downloadUrl = newJobConfigWithOverride.jobVersion?.packageInfo?.downloadUrl
                }
            }

            if(jobPackageFile && jobPackageFile.absolutePath) {
                newMappedJobData.jobVersion?.packageInfo {
                    name =  jobPackageFile.absolutePath
                }
            }

            mapClosure(newMappedJobData, job, job.key)
        }

    }

    def static Map mapJobWithoutMail(Job job, JobVersion jobVersion, String projectId) {
        if(!job || !jobVersion){
            return null
        }
        def technology = job.technology
        def mappedJob = job.toMap()
        def mappedJobVersion = jobVersion.toMap()
        def newJobConfig = [:]


        newJobConfig.put('job', [
                name : mappedJob?.name,
                cronScheduling : mappedJob?.cronScheduling,
                isScheduled : mappedJob?.isScheduled,
                category : mappedJob?.category,
                isStreaming : mappedJob?.isStreaming,
                description : mappedJob?.description,
                technology: [id: technology]
            ])

        if(projectId){
            newJobConfig.job.put("projectId", projectId)
        }

        if(mappedJob.alerting?.emails) {
            newJobConfig.job.put('alerting',[
                emails : mappedJob.alerting?.emails,
                statusList : mappedJob.alerting?.statusList,
                logins : mappedJob.alerting?.logins
            ])
        }

        newJobConfig.put('jobVersion', [
                commandLine : mappedJobVersion.commandLine,
                releaseNote : mappedJobVersion.releaseNote,
                runtimeVersion : mappedJobVersion.runtimeVersion,
                dockerInfo : [
                    image : mappedJobVersion.dockerInfo?.image,
                    dockerCredentialsId : mappedJobVersion.dockerInfo?.dockerCredentialsId
                ]
        ])

        if(jobVersion.packageInfo?.name || jobVersion.packageInfo?.downloadUrl){
            newJobConfig.jobVersion.put('packageInfo', [
                    name: jobVersion.packageInfo?.name,
                    downloadUrl: jobVersion.packageInfo?.downloadUrl
                ])
        }

        if(mappedJobVersion.userPreviousArtifact){
            newJobConfig.jobVersion.put('userPreviousArtifact', [
                userPreviousArtifact : mappedJobVersion.userPreviousArtifact
            ])
        }

        if(mappedJobVersion.resources?.cpu || mappedJobVersion.resources?.disk || mappedJobVersion.resources?.memory){
            newJobConfig.jobVersion.put('resources', mappedJobVersion.resources)
        }

        if(mappedJobVersion.extraTechnology){
            newJobConfig.jobVersion.put('extraTechnology', mappedJobVersion.extraTechnology)
        }

        if(mappedJobVersion.volume){
            newJobConfig.jobVersion.put('volume', mappedJobVersion.volume)
        }

        if(mappedJobVersion.exposedPorts){
            newJobConfig.jobVersion.put('exposedPorts', mappedJobVersion.exposedPorts)
        }

        if(mappedJobVersion.doesUseGPU){
            newJobConfig.jobVersion.put('doesUseGPU', mappedJobVersion.doesUseGPU)
        }

        return newJobConfig
    }
}
