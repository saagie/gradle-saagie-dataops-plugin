# gradle-saagie-dataops-plugin
Saagie Gradle Plugin for the new version (2.0) of Saagie Dataops Orchestrator

If you are looking for the gradle plugin for Saagie Manager 1.0 please go there : https://github.com/saagie/gradle-saagie-plugin

More informations about Saagie: https://www.saagie.com

This plugin is only compatible with gradle 3.0+

## Setup

```
plugins {
  id "io.saagie.gradle-saagie-dataops-plugin" version "1.1.68"
}
```

## Usage

The following tasks are available:
```
> gradle tasks

Saagie tasks
------------
groupList - list all groups for the user
platformList - list available platforms
projectsArchiveJob - archive a task
projectsCreate - create a brand new project
projectsCreateJob - create a brand new job in a project
projectsCreatePipeline - create a pipeline
projectsDelete - archive a project
projectsDeletePipeline - delete a pipeline
projectsGetJobInstanceStatus - get the status of a job instance
projectsGetPipelineInstanceStatus - get the status of a pipeline instance
projectsList - list all projects on the environment
projectsListJobs - list all jobs of a project
projectsListPipelines - list all pipelines of a project
projectsListTechnologies - list all technologies of a project
projectsRunJob - run an existing job
projectsRunPipeline - run a pipeline
projectsStopJobInstance - stop a job instance
projectsStopPipelineInstance - stop a pipeline instance
projectsUpdate - update an existing project
projectsUpdateJob - update a existing job in a project
projectsUpdatePipeline - update a pipeline
technologyList - list all technologies for the user
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
