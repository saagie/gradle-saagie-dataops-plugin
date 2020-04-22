package io.saagie.plugin.dataops.models

class JobMapper {
    Job job = new Job()
    JobVersion jobVersion = new JobVersion()

    public JobMapper() {
        this.jobVersion.resources {
            disk = 512
            memory = 512
            cpu = 0.3
        }
    }

    Object job(Closure closure) {
        job.with(closure)
    }

    Object jobVersion(Closure closure) {
        jobVersion.with(closure)
    }

    def static Map mapJobAndJobVersionWithoutMail(Job job, JobVersion jobVersion, String projectId) {
        if(!job){
            return null
        }
        def newJobConfig = mapJobWithoutMail(job, projectId)
        def mappedJobVersion = jobVersion.toMap()


        if(jobVersion) {
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

        }
        return newJobConfig
    }

    def static mapJobWithoutMail(Job job, String projectId) {
        def mappedJob = job.toMap()
        def newJobConfig = [:]

        def technology = job.technology


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
        return newJobConfig
    }
}
