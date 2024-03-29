package io.saagie.plugin.dataops

import io.saagie.plugin.dataops.tasks.group.GroupListTask
import io.saagie.plugin.dataops.tasks.platform.PlatformListTask
import io.saagie.plugin.dataops.tasks.projects.artifact.ProjectsExportJobTask
import io.saagie.plugin.dataops.tasks.projects.artifact.ProjectsExportJobV1Task
import io.saagie.plugin.dataops.tasks.projects.artifact.ProjectsImportJobTask
import io.saagie.plugin.dataops.tasks.projects.graphPipeline.*
import io.saagie.plugin.dataops.tasks.projects.job.*
import io.saagie.plugin.dataops.tasks.projects.jobInstance.ProjectGetJobInstanceStatusTask
import io.saagie.plugin.dataops.tasks.projects.jobInstance.ProjectStopJobInstanceTask
import io.saagie.plugin.dataops.tasks.projects.pipeline.*
import io.saagie.plugin.dataops.tasks.projects.pipelineInstance.ProjectGetPipelineInstanceStatusTask
import io.saagie.plugin.dataops.tasks.projects.pipelineInstance.ProjectStopPipelineInstanceTask
import io.saagie.plugin.dataops.tasks.projects.project.ProjectDeleteTask
import io.saagie.plugin.dataops.tasks.projects.project.ProjectListTask
import io.saagie.plugin.dataops.tasks.projects.project.ProjectsCreateTask
import io.saagie.plugin.dataops.tasks.projects.project.ProjectsUpdateTask
import io.saagie.plugin.dataops.tasks.projects.technology.ProjectListTechnologiesTask
import io.saagie.plugin.dataops.tasks.projects.technology.TechnologyListTask
import org.gradle.api.Project

class DataOpsModule {

    final static String PROJECTS_LIST_TASK = 'projectsList'
    final static String PROJECTS_LIST_JOBS_TASK = 'projectsListJobs'
    final static String PROJECTS_LIST_TECHNOLOGIES_TASK = 'projectsListTechnologies'
    final static String PROJECTS_CREATE_JOB_TASK = 'projectsCreateJob'
    final static String PROJECTS_RUN_JOB_TASK = 'projectsRunJob'
    final static String PROJECTS_UPGRADE_JOB_TASK = 'projectsUpgradeJob'
    final static String PROJECTS_GET_JOB_INSTANCE_STATUS = 'projectsGetJobInstanceStatus'
    final static String PROJECTS_CREATE_PIPELINE_TASK = 'projectsCreatePipeline'
    final static String PROJECT_DELETE_TASK = 'projectsDelete'
    final static String PROJECTS_GET_PIPELINE_INSTANCE_STATUS = 'projectsGetPipelineInstanceStatus'
    final static String PROJECTS_UPGRADE_PIPELINE_TASK = 'projectsUpgradePipeline'
    final static String PROJECTS_RUN_PIPELINE_TASK = 'projectsRunPipeline'
    final static String PROJECTS_STOP_JOB_INSTANCE_TASK = 'projectsStopJobInstance'
    final static String PROJECTS_DELETE_JOB_TASK = 'projectsDeleteJob'
    final static String PROJECTS_STOP_PIPELINE_INSTANCE_TASK = 'projectsStopPipelineInstance'
    final static String PROJECTS_DELETE_PIPELINE_TASK = 'projectsDeletePipeline'
    final static String PLATFORM_LIST_TASK = 'platformList'
    final static String PROJECTS_LIST_PIPELINES_TASK = 'projectsListPipelines'
    final static String TECHNOLOGY_LIST_TASK = 'technologyList'
    final static String GROUP_LIST_TASK = 'groupList'
    final static String PROJECTS_CREATE_TASK = 'projectsCreate'
    final static String PROJECTS_UPDATE = 'projectsUpdate'
    final static String PROJECTS_EXPORT_ARTIFACTS = 'projectsExport'
    final static String PROJECTS_EXPORT_ARTIFACTS_V1 = 'projectsExportV1'
    final static String PROJECTS_IMPORT_ARTIFACTS_JOB = 'projectsImport'
    final static String PROJECTS_CREATE_GRAPH_PIPELINE_TASK = 'projectsCreateGraphPipeline'
    final static String PROJECTS_UPGRADE_GRAPH_PIPELINE_TASK = 'projectsUpgradeGraphPipeline'
    final static String PROJECTS_LIST_GRAPH_PIPELINES_TASK = 'projectsListGraphPipelines'

    final static String TASK_GROUP = 'Saagie'

    static void load(Project project) {
        project.extensions.create('saagie', DataOpsExtension);

        project.task(PROJECTS_LIST_TASK, type: ProjectListTask) {
            group = TASK_GROUP
            description = 'List all projects on the environment'
            configuration = project.saagie
            taskName = PROJECTS_LIST_TASK
        }

        project.task(PROJECTS_LIST_JOBS_TASK, type: ProjectListJobsTask) {
            group = TASK_GROUP
            description = 'List all jobs of a project'
            configuration = project.saagie
            taskName = PROJECTS_LIST_JOBS_TASK
        }

        project.task(PROJECTS_LIST_TECHNOLOGIES_TASK, type: ProjectListTechnologiesTask) {
            group = TASK_GROUP
            description = 'List all technologies of a project'
            configuration = project.saagie
            taskName = PROJECTS_LIST_TECHNOLOGIES_TASK
        }

        project.task(PROJECTS_CREATE_JOB_TASK, type: ProjectCreateJobTask) {
            group = TASK_GROUP
            description = 'Create a brand new job in a project'
            configuration = project.saagie
            taskName = PROJECTS_CREATE_JOB_TASK
        }

        project.task(PROJECTS_UPGRADE_JOB_TASK, type: ProjectUpgradeJobTask) {
            group = TASK_GROUP
            description = 'Upgrade a existing job in a project'
            configuration = project.saagie
            taskName = PROJECTS_UPGRADE_JOB_TASK
        }

        project.task(PROJECTS_RUN_JOB_TASK, type: ProjectRunJobTask) {
            group = TASK_GROUP
            description = 'Run an existing job'
            configuration = project.saagie
            taskName = PROJECTS_RUN_JOB_TASK
        }

        project.task(PROJECTS_GET_JOB_INSTANCE_STATUS, type: ProjectGetJobInstanceStatusTask) {
            group = TASK_GROUP
            description = 'Get the status of a job instance'
            configuration = project.saagie
            taskName = PROJECTS_GET_JOB_INSTANCE_STATUS
        }

        project.task(PROJECTS_CREATE_PIPELINE_TASK, type: ProjectCreatePipelineTask) {
            group = TASK_GROUP
            description = 'Create a pipeline'
            configuration = project.saagie
            taskName = PROJECTS_CREATE_PIPELINE_TASK
        }

        project.task(PROJECT_DELETE_TASK, type: ProjectDeleteTask) {
            group = TASK_GROUP
            description = 'Archive a project'
            configuration = project.saagie
            taskName = PROJECT_DELETE_TASK
        }

        project.task(PROJECTS_EXPORT_ARTIFACTS, type: ProjectsExportJobTask) {
            group = TASK_GROUP
            description = 'Export a list of jobs, pipelines and environments variables for a project to a zip format'
            configuration = project.saagie
            taskName = PROJECTS_EXPORT_ARTIFACTS
        }

        project.task(PROJECTS_EXPORT_ARTIFACTS_V1, type: ProjectsExportJobV1Task) {
            group = TASK_GROUP
            description = 'Export a list of jobs, pipelines and environment variables from Manager to a zip format so it can be imported into V2 ( projects )'
            configuration = project.saagie
            taskName = PROJECTS_EXPORT_ARTIFACTS_V1
        }

        project.task(PROJECTS_GET_PIPELINE_INSTANCE_STATUS, type: ProjectGetPipelineInstanceStatusTask) {
            group = TASK_GROUP
            description = 'Get the status of a pipeline instance'
            configuration = project.saagie
            taskName = PROJECTS_GET_PIPELINE_INSTANCE_STATUS
        }

        project.task(PROJECTS_UPGRADE_PIPELINE_TASK, type: ProjectUpgradePipelineTask) {
            group = TASK_GROUP
            description = 'Upgrade a pipeline'
            configuration = project.saagie
            taskName = PROJECTS_UPGRADE_PIPELINE_TASK
        }

        project.task(PROJECTS_RUN_PIPELINE_TASK, type: ProjectRunPipelineTask) {
            group = TASK_GROUP
            description = 'Run a pipeline'
            configuration = project.saagie
            taskName = PROJECTS_RUN_PIPELINE_TASK
        }

        project.task(PROJECTS_STOP_JOB_INSTANCE_TASK, type: ProjectStopJobInstanceTask) {
            group = TASK_GROUP
            description = 'Stop a job instance'
            configuration = project.saagie
            taskName = PROJECTS_STOP_JOB_INSTANCE_TASK
        }

        project.task(PROJECTS_DELETE_JOB_TASK, type: ProjectDeleteJobTask) {
            group = TASK_GROUP
            description = 'Delete a job'
            configuration = project.saagie
            taskName = PROJECTS_DELETE_JOB_TASK
        }

        project.task(PROJECTS_STOP_PIPELINE_INSTANCE_TASK, type: ProjectStopPipelineInstanceTask) {
            group = TASK_GROUP
            description = 'Stop a pipeline instance'
            configuration = project.saagie
            taskName = PROJECTS_STOP_PIPELINE_INSTANCE_TASK
        }

        project.task(PROJECTS_DELETE_PIPELINE_TASK, type: ProjectDeletePipelineTask) {
            group = TASK_GROUP
            description = 'delete a pipeline'
            configuration = project.saagie
            taskName = PROJECTS_DELETE_PIPELINE_TASK
        }

        project.task(PLATFORM_LIST_TASK, type: PlatformListTask) {
            group = TASK_GROUP
            description = 'List available platforms'
            configuration = project.saagie
            taskName = PLATFORM_LIST_TASK
        }

        project.task(PROJECTS_LIST_PIPELINES_TASK, type: ProjectsListPipelinesTask) {
            group = TASK_GROUP
            description = 'List all pipelines of a project'
            configuration = project.saagie
            taskName = PROJECTS_LIST_PIPELINES_TASK
        }

        project.task(TECHNOLOGY_LIST_TASK, type: TechnologyListTask) {
            group = TASK_GROUP
            description = 'List all technologies for the user'
            configuration = project.saagie
            taskName = TECHNOLOGY_LIST_TASK
        }

        project.task(GROUP_LIST_TASK, type: GroupListTask) {
            group = TASK_GROUP
            description = 'List all groups for the user'
            configuration = project.saagie
            taskName = GROUP_LIST_TASK
        }

        project.task(PROJECTS_CREATE_TASK, type: ProjectsCreateTask) {
            group = TASK_GROUP
            description = 'Create a brand new project'
            configuration = project.saagie
            taskName = PROJECTS_CREATE_TASK
        }

        project.task(PROJECTS_UPDATE, type: ProjectsUpdateTask) {
            group = TASK_GROUP
            description = 'Update an existing project'
            configuration = project.saagie
            taskName = PROJECTS_UPDATE
        }

        project.task(PROJECTS_IMPORT_ARTIFACTS_JOB, type: ProjectsImportJobTask) {
            group = TASK_GROUP
            description = 'Import a List of jobs, pipelines or environment variables using the artifacts from a zip location'
            configuration = project.saagie
            taskName = PROJECTS_IMPORT_ARTIFACTS_JOB
        }

        project.task(PROJECTS_CREATE_GRAPH_PIPELINE_TASK, type: ProjectCreateGraphPipelineTask) {
            group = TASK_GROUP
            description = 'Create a graph pipeline'
            configuration = project.saagie
            taskName = PROJECTS_CREATE_GRAPH_PIPELINE_TASK
        }

        project.task(PROJECTS_UPGRADE_GRAPH_PIPELINE_TASK, type: ProjectUpgradeGraphPipelineTask) {
            group = TASK_GROUP
            description = 'Upgrade a graph pipeline'
            configuration = project.saagie
            taskName = PROJECTS_UPGRADE_GRAPH_PIPELINE_TASK
        }

        project.task(PROJECTS_LIST_GRAPH_PIPELINES_TASK, type: ProjectsListGraphPipelinesTask) {
            group = TASK_GROUP
            description = 'List all graph pipelines of a project'
            configuration = project.saagie
            taskName = PROJECTS_LIST_GRAPH_PIPELINES_TASK
        }
    }
}
