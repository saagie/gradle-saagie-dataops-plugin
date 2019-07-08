package io.saagie.plugin.dataops

import io.saagie.plugin.dataops.models.Job
import io.saagie.plugin.dataops.models.JobVersion
import io.saagie.plugin.dataops.models.Project
import io.saagie.plugin.dataops.models.Server

class DataOpsExtension {
    Server server = new Server()
    Project project = new Project()
    Job job = new Job()
    JobVersion jobVersion = new JobVersion()

    Server server(Closure closure) {
        server.with(closure)
    }

    Project project(Closure closure) {
        project.with(closure)
    }

    Job job(Closure closure) {
        job.with(closure)
    }

    JobVersion jobVersion(Closure closure) {
        jobVersion.with(closure)
    }
}
