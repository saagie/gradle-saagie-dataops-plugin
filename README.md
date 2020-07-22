# gradle-saagie-dataops-plugin
Saagie Gradle Plugin for the new version (2.0) of Saagie Dataops Orchestrator

If you are looking for the gradle plugin for Saagie Manager 1.0 please go there : https://github.com/saagie/gradle-saagie-plugin

More informations about Saagie: https://www.saagie.com

This plugin is only compatible with gradle 3.0+

## Setup

```
plugins {
  id "io.saagie.gradle-saagie-dataops-plugin" version "2.0.27"
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
projectsCreatePipeline - Create a pipeline
projectsDelete - Archive a project
projectsDeleteJob - Delete a job
projectsDeletePipeline - delete a pipeline
projectsExport - Export a list of jobs or pipelines for a project to a zip extension
projectsGetJobInstanceStatus - Get the status of a job instance
projectsGetPipelineInstanceStatus - Get the status of a pipeline instance
projectsImport - Import a List of jobs or pipelines using the artifacts from a zip location
projectsList - List all projects on the environment
projectsListJobs - List all jobs of a project
projectsListPipelines - List all pipelines of a project
projectsListTechnologies - List all technologies of a project
projectsRunJob - Run an existing job
projectsRunPipeline - Run a pipeline
projectsStopJobInstance - Stop a job instance
projectsStopPipelineInstance - Stop a pipeline instance
projectsUpdate - Update an existing project
projectsUpgradeJob - Upgrade a existing job in a project
projectsUpgradePipeline - Upgrade a pipeline
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
