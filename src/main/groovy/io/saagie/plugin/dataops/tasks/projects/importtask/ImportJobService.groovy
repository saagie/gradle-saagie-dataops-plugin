package io.saagie.plugin.dataops.tasks.projects.importtask

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Job
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

                if(globalConfig.jobOverride.alerting && globalConfig.jobOverride.alerting.emails){
                    newJobConfigWithOverride << [*:[alerting : globalConfig.jobOverride?.alerting]]
                }

            }

            globalConfig.job {
                name = newJobConfigWithOverride.name
                technology = newJobConfigWithOverride.technology
                cronScheduling = newJobConfigWithOverride.cronScheduling
                isScheduled =  newJobConfigWithOverride.isScheduled
                category = newJobConfigWithOverride.category
                description = newJobConfigWithOverride.description
            }

            if(newJobConfigWithOverride.alerting?.emails) {
                globalConfig.job.alerting {
                    emails = newJobConfigWithOverride.alerting?.emails
                    statusList = newJobConfigWithOverride.alerting?.statusList
                    logins = newJobConfigWithOverride.alerting?.logins
                }
            }

            globalConfig.jobVersion {
                commandLine = jobConfigOverride.jobVersion.commandLine
                releaseNote = jobConfigOverride.jobVersion.releaseNote
                runtimeVersion = jobConfigOverride.jobVersion.runtimeVersion
                dockerInfo {
                    image = jobConfigOverride.jobVersion.dockerInfo?.image
                    dockerCredentialsId = jobConfigOverride.jobVersion.dockerInfo?.dockerCredentialsId
                }
                packageInfo {
                    name = jobPackageFile.absolutePath
                }
            }
            mapClosure(globalConfig, job, job.key)
        }

    }
}
