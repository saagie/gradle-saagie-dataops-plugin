package io.saagie.plugin.dataops.tasks.service.importTask

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.JobMapper
import io.saagie.plugin.dataops.models.JobVersion

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
            def versions = null
            def newJobConfigWithOverride = [
                *: jobConfigOverride.job
            ]

            if (globalConfig.jobOverride) {
                if (globalConfig.jobOverride?.isScheduled) {
                    newJobConfigWithOverride << [*: [
                        isScheduled: globalConfig.jobOverride?.isScheduled
                    ]];
                }

                if (globalConfig.jobOverride?.cronScheduling) {
                    newJobConfigWithOverride << [*: [
                        cronScheduling: globalConfig.jobOverride?.cronScheduling
                    ]];
                }

                if (globalConfig.jobOverride.alerting?.emails) {
                    newJobConfigWithOverride << [*: [alerting: globalConfig.jobOverride?.alerting]]
                }
            }


            JobMapper newMappedJobData = new JobMapper()

            newMappedJobData.job {
                name = newJobConfigWithOverride.name
                technology = newJobConfigWithOverride.technology
                cronScheduling = newJobConfigWithOverride.cronScheduling
                isScheduled = newJobConfigWithOverride.isScheduled
                category = newJobConfigWithOverride.category
                description = newJobConfigWithOverride.description
            }

            if (newJobConfigWithOverride.alerting?.emails) {
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

            if (jobPackageFile && jobPackageFile.absolutePath) {
                newMappedJobData.jobVersion?.packageInfo {
                    name = jobPackageFile.absolutePath
                }
            }

            if (jobConfigOverride.jobVersion?.extraTechnology?.language) {
                newMappedJobData.jobVersion?.extraTechnology {
                    language = jobConfigOverride.jobVersion?.extraTechnology?.language
                    version = jobConfigOverride.jobVersion?.extraTechnology?.version
                }
            }

            if (jobConfigOverride.versions && jobConfigOverride.versions.size() > 0) {
                versions = jobConfigOverride.versions
            }


            if (newJobConfigWithOverride.jobVersion?.packageInfo) {
                newMappedJobData.jobVersion?.packageInfo {
                    downloadUrl = newJobConfigWithOverride.jobVersion?.packageInfo?.downloadUrl
                }
            }

            mapClosure(newMappedJobData, job, job.key, versions, newJobConfigWithOverride.technologyName)
        }

    }

    static convertFromMapToJsonVersion(jobVersionMap) {
        JobVersion jobVersion = []
        jobVersion.with {
            commandLine = jobVersionMap.commandLine
            releaseNote = jobVersionMap.releaseNote
            runtimeVersion = jobVersionMap.runtimeVersion
            dockerInfo {
                image = jobVersionMap.dockerInfo?.image
                dockerCredentialsId = jobVersionMap.dockerInfo?.dockerCredentialsId
            }
        }

        if (jobVersionMap?.packageInfo?.name) {
            jobVersion.packageInfo {
                name = jobVersionMap.packageInfo.name
            }
        }

        if (jobVersionMap?.extraTechnology?.language) {
            jobVersion.extraTechnology {
                language = jobVersionMap?.extraTechnology?.language
                version = jobVersionMap?.extraTechnology?.version
            }
        }

        return jobVersion
    }

}
