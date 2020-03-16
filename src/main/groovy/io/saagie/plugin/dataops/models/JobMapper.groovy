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
}
