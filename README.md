# gradle-saagie-dataops-plugin
Saagie Gradle Plugin for the new version (2.0) of Saagie Dataops Orchestrator

If you are looking for the gradle plugin for Saagie Manager 1.0 please go there : https://github.com/saagie/gradle-saagie-plugin

More informations about Saagie: https://www.saagie.com

This plugin is only compatible with Gradle 5.1 to 6.x (not compatible with Gradle 7.0+)


## Setup

```
plugins {
  id "io.saagie.gradle-saagie-dataops-plugin" version "2.3.0"
}
```

## Usage

The following tasks are available:
```
> gradle tasks

Saagie tasks
------------
groupList - List all groups for the user
platformList - List available platforms
projectsCreate - Create a brand new project
projectsCreateJob - Create a brand new job in a project
projectsCreatePipeline - Create a linear pipeline
projectsCreateGraphPipeline - Create a pipeline with a graph
projectsDelete - Delete a project
projectsDeleteJob - Delete a job
projectsDeletePipeline - Delete a pipeline
projectsExport - Export a list of jobs, pipelines and environments variables for a project to a zip format
projectsExportV1 - Export a list of jobs, pipelines and environment variables from Manager to a zip format so it can be imported into V2 ( projects )
projectsGetJobInstanceStatus - Get the status of a job instance
projectsGetPipelineInstanceStatus - Get the status of a pipeline instance
projectsImport - Import a List of jobs, pipelines or environment variables using the artifacts from a zip location
projectsList - List all projects on the environment
projectsListJobs - List all jobs of a project
projectsListPipelines - List all linears pipelines of a project
projectsListGraphPipelines - List all pipelines (graph and linears) of a project, in the form of a graph
projectsListTechnologies - List all technologies of a project
projectsRunJob - Run an existing job
projectsRunPipeline - Run a pipeline
projectsStopJobInstance - Stop a job instance
projectsStopPipelineInstance - Stop a pipeline instance
projectsUpdate - Update an existing project
projectsUpgradeJob - Upgrade a existing job in a project
projectsUpgradePipeline - Upgrade a linear pipeline
projectsUpgradeGraphPipeline - Upgrade a graph pipeline
technologyList - List all technologies for the user
```

## Quick Example
```
saagie {
    server {
        url = 'https://saagie-beta.prod.saagie.io'
        login = 'my-login'
        password = 'my-password'
        environment = 4
    }
    project {
        id = '2dc84971-6c9c-4500-8be1-9c7faff19f7b'
    }
}
```
Then launch command ```gradle projectsListJobs```

## Debug
```
gradle projectsList -w # launch in warn mode
gradle projectsList -i # launch in info mode
gradle projectsList -d # launch in debug mode
```

## Documentation
Full documentation is available on the [wiki](https://github.com/saagie/gradle-saagie-dataops-plugin/wiki)

## Changelog

Changelogs are available [here](https://github.com/saagie/gradle-saagie-dataops-plugin/releases)
