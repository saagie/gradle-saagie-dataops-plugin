package io.saagie.plugin.dataops

import groovy.transform.TypeChecked
import io.saagie.plugin.dataops.models.Job
import io.saagie.plugin.dataops.models.JobInstance
import io.saagie.plugin.dataops.models.JobVersion
import io.saagie.plugin.dataops.models.PipelineInstance
import io.saagie.plugin.dataops.models.Pipeline
import io.saagie.plugin.dataops.models.PipelineVersion
import io.saagie.plugin.dataops.models.Project
import io.saagie.plugin.dataops.models.Server

import javax.validation.constraints.NotNull

@TypeChecked
class DataOpsExtension {

    @NotNull(message = 'server config cannot be empty')
    Server server = new Server()

    Project project = new Project()

    Job job = new Job()

    JobVersion jobVersion = new JobVersion()

    JobInstance jobinstance = new JobInstance()

    PipelineInstance pipelineInstance = new PipelineInstance()

    Pipeline pipeline = new Pipeline()

    PipelineVersion pipelineVersion = new PipelineVersion()

    Object server(Closure closure) {
        server.with(closure)
    }

    Object project(Closure closure) {
        project.with(closure)
    }

    Object job(Closure closure) {
        job.with(closure)
    }

    Object jobVersion(Closure closure) {
        jobVersion.with(closure)
    }

    Object jobinstance(Closure closure) {
        jobinstance.with(closure)
    }

    Object pipeline(Closure closure) {
        pipeline.with(closure)
    }

    Object pipelineVersion(Closure closure) {
        pipelineVersion.with(closure)
    }

    Object pipelineInstance(Closure closure) {
        pipelineInstance.with(closure)
    }
}
