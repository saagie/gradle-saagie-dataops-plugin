package io.saagie.plugin.dataops

import groovy.transform.TypeChecked
import io.saagie.plugin.dataops.models.App
import io.saagie.plugin.dataops.models.ArtifactPropertyOverride
import io.saagie.plugin.dataops.models.EnvVariable
import io.saagie.plugin.dataops.models.Export
import io.saagie.plugin.dataops.models.ImportJob
import io.saagie.plugin.dataops.models.Job
import io.saagie.plugin.dataops.models.JobInstance
import io.saagie.plugin.dataops.models.JobVersion
import io.saagie.plugin.dataops.models.PipelineInstance
import io.saagie.plugin.dataops.models.Pipeline
import io.saagie.plugin.dataops.models.PipelineVersion
import io.saagie.plugin.dataops.models.Project
import io.saagie.plugin.dataops.models.PropertyOverride
import io.saagie.plugin.dataops.models.Server
import io.saagie.plugin.dataops.models.graphPipeline.GraphPipelineVersion

import javax.validation.constraints.NotNull

@TypeChecked
class DataOpsExtension {

    @NotNull(message = 'server config cannot be empty')
    Server server = new Server()

    Project project = new Project()

    Job job = new Job()

    JobVersion jobVersion = new JobVersion()

    JobInstance jobinstance = new JobInstance()

    PipelineInstance pipelineinstance = new PipelineInstance()

    App apps =  new App()

    Pipeline pipeline = new Pipeline()

    PipelineVersion pipelineVersion = new PipelineVersion()

    GraphPipelineVersion graphPipelineVersion = new GraphPipelineVersion()

    ImportJob importArtifacts = new ImportJob()

    ArtifactPropertyOverride jobOverride = new ArtifactPropertyOverride()

    ArtifactPropertyOverride pipelineOverride = new ArtifactPropertyOverride()

    PropertyOverride propertyOverride = new PropertyOverride()

    Export exportArtifacts = new Export()

    EnvVariable env = new EnvVariable()

    Object newTaskContext = new Object()

    // ====== Closures to create a proper DSL
    Object server(Closure closure) {
        server = new Server()
        server.with(closure)
    }

    Object newTaskContext(Closure closure) {
        project = new Project()
        job = new Job()
        jobVersion = new JobVersion()
        jobinstance = new JobInstance()
        pipeline = new Pipeline()
        env = new EnvVariable()
        pipelineVersion = new PipelineVersion()
        graphPipelineVersion = new GraphPipelineVersion()
        pipelineinstance = new PipelineInstance()
        importArtifacts = new ImportJob()
        jobOverride = new ArtifactPropertyOverride()
        pipelineOverride = new ArtifactPropertyOverride()
        exportArtifacts = new Export()
        newTaskContext.with(closure)
    }

    Object apps(Closure closure) {
        apps = new App()
        apps.with(closure)
    }
    Object propertyOverride(Closure closure) {
        propertyOverride = new PropertyOverride()
        propertyOverride.with(closure)
    }

    Object project(Closure closure) {
        project = new Project()
        project.with(closure)
    }

    Object job(Closure closure) {
        job = new Job()
        job.with(closure)
    }

    Object jobVersion(Closure closure) {
        jobVersion = new JobVersion()
        jobVersion.with(closure)
    }

    Object jobinstance(Closure closure) {
        jobinstance = new JobInstance()
        jobinstance.with(closure)
    }

    Object pipeline(Closure closure) {
        pipeline = new Pipeline()
        pipeline.with(closure)
    }

    Object env(Closure closure) {
        env = new EnvVariable()
        env.with(closure)
    }

    Object pipelineVersion(Closure closure) {
        pipelineVersion = new PipelineVersion()
        pipelineVersion.with(closure)
    }

    Object graphPipelineVersion(Closure closure) {
        graphPipelineVersion = new GraphPipelineVersion()
        graphPipelineVersion.with(closure)
    }

    Object pipelineinstance(Closure closure) {
        pipelineinstance = new PipelineInstance()
        pipelineinstance.with(closure)
    }

    Object importArtifacts(Closure closure) {
        importArtifacts = new ImportJob()
        importArtifacts.with(closure)
    }

    Object jobOverride(Closure closure) {
        jobOverride = new ArtifactPropertyOverride()
        jobOverride.with(closure)
    }

    Object pipelineOverride(Closure closure) {
        pipelineOverride = new ArtifactPropertyOverride()
        pipelineOverride.with(closure)
    }

    Object exportArtifacts(Closure closure) {
        exportArtifacts = new Export()
        exportArtifacts.with(closure)
    }
}
