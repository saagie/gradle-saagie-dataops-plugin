# gradle-saagie-dataops-plugin
Saagie Gradle Plugin for the new version (2.0) of Saagie Dataops Orchestrator

If you are looking for the gradle plugin for Saagie Manager 1.0 please go there : https://github.com/saagie/gradle-saagie-plugin

More informations about Saagie: https://www.saagie.com

This plugin is only compatible with gradle 3.0+

## Setup

```
plugins {
  id "io.saagie.gradle-saagie-dataops-plugin" version "1.1.13"
}
```

## Usage

The following tasks are available:

| Tasks                             | Description                                                             | Availability |
|-----------------------------------|-------------------------------------------------------------------------|--------------|
| projectsList                      | Lists all projects of a platform                                        | Available    |
| projectsCreate                    | Creates a project                                                       | To Do        |
| projectsUpdate                    | Update an existing project                                              | To Do        |
| projectsArchive                   | Archive an existing project                                             | To Do        |
| projectsListTechnologies          | Lists all technologies of a project                                     | Available    |
| projectsListJobs                  | Lists all jobs of a project                                             | Available    |
| projectsCreateJob                 | Creates a new job in a project                                          | Available    |
| projectsUpdateJob                 | Updates an existing job                                                 | Available    |
| projectsRunJob                    | Run an existing job                                                     | Available    |
| projectsStopJobInstance           | Stops an existing job instance                                          | Available    |
| projectsGetJobInstanceStatus      | List instances of a job                                                 | Available    |
| projectsListJobInstances          | List instances of a job                                                 | To Do        |
| projectsGetJobInstanceLog         | Get log of a job instance                                               | To Do        |
| projectsExportJob                 | Exports an existing job                                                 | To Do        |
| projectsExportAllJobs             | Export all jobs of project                                              | To Do        |
| projectsImportJob                 | Import a job                                                            | To Do        |
| projectsImportAllJobs             | Impact a set of jobs in a project                                       | To Do        |
| projectsArchiveJob                | Archive an existing job                                                 | Available    |
| projectsListPipelines             | Lists all pipelines on the project                                      | To Do        |
| projectsCreatePipeline            | Creates a new pipeline in a project                                     | Available    |
| projectsUpdatePipeline            | Updates an existing pipeline                                            | Available    |
| projectsRunPipeline               | Run an existing pipeline                                                | Available    |
| projectsStopPipelineInstance      | Stops an existing pipeline instance                                     | Available    |
| projectsExportPipeline            | Exports an existing pipeline                                            | To Do        |
| projectsExportAllPipelines        | Export all pipelines of project                                         | To Do        |
| projectsImportPipeline            | Import a pipeline                                                       | To Do        |
| projectsImportAllPipelines        | Impact a set of pipelines in a project                                  | To Do        |
| projectsDeletePipeline            | Delete an existing pipeline                                             | Available    |
| projectsListGlobalVariables       | List all global variables                                               | To Do        |
| projectsListProjectVariables      | List project variables                                                  | To Do        |
| projectsCreateVariable            | Creates a variable                                                      | To Do        |
| projectsUpdateVariable            | Updates a variable                                                      | To Do        |
| projectsDeleteVariable            | Delete a variable                                                       | To Do        |
| projectsExportAllVariable         | Export variable into a local file (global only, project only, both)     | To Do        |
| projectsImportAllVariable         | Import variable from a local file specifying target global or project   | To Do        |
| governanceListDomains             | List all domains                                                        | To Do        |
| governanceCreateDomain            | Create a new domain                                                     | To Do        |
| governanceListDatasets            | List datasets based on search criterias                                 | To Do        |
| governanceGetDataset              | Get meta data details of dataset                                        | To Do        |
| governanceUpdateDataset           | Update meta data of a dataset                                           | To Do        |



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

## Documentation
Full documentation is available on the [wiki](https://github.com/saagie/gradle-saagie-dataops-plugin/wiki)

## Changelog

Changelogs are available [here](https://github.com/saagie/gradle-saagie-dataops-plugin/releases)
