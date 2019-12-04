package io.saagie.plugin.dataops

import io.saagie.plugin.dataops.tasks.group.GroupListTask
import io.saagie.plugin.dataops.tasks.projects.ProjectArchiveJobTask
import io.saagie.plugin.dataops.tasks.projects.ProjectCreateJobTask
import io.saagie.plugin.dataops.tasks.projects.ProjectCreatePipelineTask
import io.saagie.plugin.dataops.tasks.projects.ProjectDeletePipelineTask
import io.saagie.plugin.dataops.tasks.projects.ProjectDeleteTask
import io.saagie.plugin.dataops.tasks.projects.ProjectGetJobInstanceStatus
import io.saagie.plugin.dataops.tasks.projects.ProjectGetPipelineInstanceStatusTask
import io.saagie.plugin.dataops.tasks.projects.ProjectListJobsTask
import io.saagie.plugin.dataops.tasks.projects.ProjectListTask
import io.saagie.plugin.dataops.tasks.projects.ProjectListTechnologiesTask
import io.saagie.plugin.dataops.tasks.projects.ProjectRunJobTask
import io.saagie.plugin.dataops.tasks.projects.ProjectRunPipelineTask
import io.saagie.plugin.dataops.tasks.projects.ProjectStopJobInstanceTask
import io.saagie.plugin.dataops.tasks.projects.ProjectStopPipelineInstanceTask
import io.saagie.plugin.dataops.tasks.projects.ProjectUpdateJob
import io.saagie.plugin.dataops.tasks.projects.ProjectUpdatePipelineTask
import io.saagie.plugin.dataops.tasks.platform.PlatformListTask
import io.saagie.plugin.dataops.tasks.projects.ProjectsCreateTask
import io.saagie.plugin.dataops.tasks.projects.ProjectsListPipelinesTask
import io.saagie.plugin.dataops.tasks.projects.ProjectsUpdateTask
import io.saagie.plugin.dataops.tasks.projects.TechnologyListTask
import org.gradle.api.Project

class DataOpsModule {

    final static String PROJECTS_LIST_TASK = 'projectsList'
    final static String PROJECTS_LIST_JOBS_TASK = 'projectsListJobs'
    final static String PROJECTS_LIST_TECHNOLOGIES_TASK = 'projectsListTechnologies'
    final static String PROJECTS_CREATE_JOB_TASK = 'projectsCreateJob'
    final static String PROJECTS_RUN_JOB_TASK = 'projectsRunJob'
    final static String PROJECTS_UPDATE_JOB_TASK = 'projectsUpdateJob'
    final static String PROJECTS_GET_JOB_INSTANCE_STATUS = 'projectsGetJobInstanceStatus'
    final static String PROJECTS_CREATE_PIPELINE_TASK = 'projectsCreatePipeline'
    final static String PROJECT_DELETE_TASK = 'projectsDelete'
    final static String PROJECTS_GET_PIPELINE_INSTANCE_STATUS = 'projectsGetPipelineInstanceStatus'
    final static String PROJECTS_UPDATE_PIPELINE_TASK = 'projectsUpdatePipeline'
    final static String PROJECTS_RUN_PIPELINE_TASK = 'projectsRunPipeline'
    final static String PROJECTS_STOP_JOB_INSTANCE_TASK = 'projectsStopJobInstance'
    final static String PROJECTS_ARCHIVE_JOB_TASK = 'projectsArchiveJob'
    final static String PROJECTS_STOP_PIPELINE_INSTANCE_TASK = 'projectsStopPipelineInstance'
    final static String PROJECTS_DELETE_PIPELINE_TASK = 'projectsDeletePipeline'
    final static String PLATFORM_LIST_TASK = 'platformList'
    final static String PROJECTS_LIST_PIPELINES_TASK = 'projectsListPipelines'
    final static String TECHNOLOGY_LIST_TASK = 'technologyList'
    final static String GROUP_LIST_TASK = 'groupList'
    final static String PROJECTS_CREATE_TASK = 'projectsCreate'
    final static String PROJECTS_UPDATE = 'projectsUpdate'

    final static String TASK_GROUP = 'Saagie'

    static void load(Project project) {
        project.extensions.create('saagie', DataOpsExtension);
        project.ext.SaagieDataOpsExtension = DataOpsExtension;

        project.task(PROJECTS_LIST_TASK, type: ProjectListTask) {
            group = TASK_GROUP
            description = 'list all projects on the environment'
            configuration = project.saagie
            taskName = PROJECTS_LIST_TASK
        }

        project.task(PROJECTS_LIST_JOBS_TASK, type: ProjectListJobsTask) {
            group = TASK_GROUP
            description = 'list all jobs of a project'
            configuration = project.saagie
            taskName = PROJECTS_LIST_JOBS_TASK
        }

        project.task(PROJECTS_LIST_TECHNOLOGIES_TASK, type: ProjectListTechnologiesTask) {
            group = TASK_GROUP
            description = 'list all technologies of a project'
            configuration = project.saagie
            taskName = PROJECTS_LIST_TECHNOLOGIES_TASK
        }

        project.task(PROJECTS_CREATE_JOB_TASK, type: ProjectCreateJobTask) {
            group = TASK_GROUP
            description = 'create a brand new job in a project'
            configuration = project.saagie
            taskName = PROJECTS_CREATE_JOB_TASK
        }

        project.task(PROJECTS_UPDATE_JOB_TASK, type: ProjectUpdateJob) {
            group = TASK_GROUP
            description = 'update a existing job in a project'
            configuration = project.saagie
            taskName = PROJECTS_UPDATE_JOB_TASK
        }

        project.task(PROJECTS_RUN_JOB_TASK, type: ProjectRunJobTask) {
            group = TASK_GROUP
            description = 'run an existing job'
            configuration = project.saagie
            taskName = PROJECTS_RUN_JOB_TASK
        }

        project.task(PROJECTS_GET_JOB_INSTANCE_STATUS, type: ProjectGetJobInstanceStatus) {
            group = TASK_GROUP
            description = 'get the status of a job instance'
            configuration = project.saagie
            taskName = PROJECTS_GET_JOB_INSTANCE_STATUS
        }

        project.task(PROJECTS_CREATE_PIPELINE_TASK, type: ProjectCreatePipelineTask) {
            group = TASK_GROUP
            description = 'create a pipeline'
            configuration = project.saagie
            taskName = PROJECTS_CREATE_PIPELINE_TASK
        }

        project.task(PROJECT_DELETE_TASK, type: ProjectDeleteTask) {
            group = TASK_GROUP
            description = 'archive a project'
            configuration = project.saagie
            taskName = PROJECT_DELETE_TASK
        }

        project.task(PROJECTS_GET_PIPELINE_INSTANCE_STATUS, type: ProjectGetPipelineInstanceStatusTask) {
            group = TASK_GROUP
            description = 'get the status of a pipeline instance'
            configuration = project.saagie
            taskName = PROJECTS_GET_PIPELINE_INSTANCE_STATUS
        }

        project.task(PROJECTS_UPDATE_PIPELINE_TASK, type: ProjectUpdatePipelineTask) {
            group = TASK_GROUP
            description = 'update a pipeline'
            configuration = project.saagie
            taskName = PROJECTS_UPDATE_PIPELINE_TASK
        }

        project.task(PROJECTS_RUN_PIPELINE_TASK, type: ProjectRunPipelineTask) {
            group = TASK_GROUP
            description = 'run a pipeline'
            configuration = project.saagie
            taskName = PROJECTS_RUN_PIPELINE_TASK
        }

        project.task(PROJECTS_STOP_JOB_INSTANCE_TASK, type: ProjectStopJobInstanceTask) {
            group = TASK_GROUP
            description = 'stop a job instance'
            configuration = project.saagie
            taskName = PROJECTS_STOP_JOB_INSTANCE_TASK
        }

        project.task(PROJECTS_ARCHIVE_JOB_TASK, type: ProjectArchiveJobTask) {
            group = TASK_GROUP
            description = 'archive a task'
            configuration = project.saagie
            taskName = PROJECTS_ARCHIVE_JOB_TASK
        }

        project.task(PROJECTS_STOP_PIPELINE_INSTANCE_TASK, type: ProjectStopPipelineInstanceTask) {
            group = TASK_GROUP
            description = 'stop a pipeline instance'
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
            description = 'list available platforms'
            configuration = project.saagie
            taskName = PLATFORM_LIST_TASK
        }

        project.task(PROJECTS_LIST_PIPELINES_TASK, type: ProjectsListPipelinesTask) {
            group = TASK_GROUP
            description = 'list all pipelines of a project'
            configuration = project.saagie
            taskName = PROJECTS_LIST_PIPELINES_TASK
        }

        project.task(TECHNOLOGY_LIST_TASK, type: TechnologyListTask) {
            group = TASK_GROUP
            description = 'list all technologies for the user'
            configuration = project.saagie
            taskName = TECHNOLOGY_LIST_TASK
        }

        project.task(GROUP_LIST_TASK, type: GroupListTask) {
            group = TASK_GROUP
            description = 'list all groups for the user'
            configuration = project.saagie
            taskName = GROUP_LIST_TASK
        }

        project.task(PROJECTS_CREATE_TASK, type: ProjectsCreateTask) {
            group = TASK_GROUP
            description = 'create a brand new project'
            configuration = project.saagie
            taskName = PROJECTS_CREATE_TASK
        }

        project.task(PROJECTS_UPDATE, type: ProjectsUpdateTask) {
            group = TASK_GROUP
            description = 'update an existing project'
            configuration = project.saagie
            taskName = PROJECTS_UPDATE
        }
    }
}
