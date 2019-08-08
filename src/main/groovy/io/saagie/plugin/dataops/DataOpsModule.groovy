package io.saagie.plugin.dataops

import io.saagie.plugin.dataops.tasks.ProjectCreateJobTask
import io.saagie.plugin.dataops.tasks.ProjectCreatePipelineTask
import io.saagie.plugin.dataops.tasks.ProjectGetJobInstanceStatus
import io.saagie.plugin.dataops.tasks.ProjectGetPipelineInstanceStatusTask
import io.saagie.plugin.dataops.tasks.ProjectListJobsTask
import io.saagie.plugin.dataops.tasks.ProjectListTask
import io.saagie.plugin.dataops.tasks.ProjectListTechnologiesTask
import io.saagie.plugin.dataops.tasks.ProjectRunJobTask
import io.saagie.plugin.dataops.tasks.ProjectRunPipelineTask
import io.saagie.plugin.dataops.tasks.ProjectUpdateJob
import io.saagie.plugin.dataops.tasks.ProjectUpdatePipelineTask
import org.gradle.api.Project

class DataOpsModule {

    final static String PROJECT_LIST_TASK = 'projectsList'
    final static String PROJECT_LIST_JOBS_TASK = 'projectsListJobs'
    final static String PROJECT_LIST_TECHNOLOGIES_TASK = 'projectsListTechnologies'
    final static String PROJECT_CREATE_JOB_TASK = 'projectsCreateJob'
    final static String PROJECT_RUN_JOB_TASK = 'projectsRunJob'
    final static String PROJECT_UPDATE_JOB_TASK = 'projectsUpdateJob'
    final static String PROJECTS_GET_JOB_INSTANCE_STATUS = 'projectsGetJobInstanceStatus'
    final static String PROJECT_CREATE_PIPELINE_TASK = 'projectsCreatePipeline'
    final static String PROJECTS_GET_PIPELINE_INSTANCE_STATUS = 'projectsGetPipelineInstanceStatus'
    final static String PROJECT_UPDATE_PIPELINE_TASK = 'projectsUpdatePipeline'
    final static String PROJECT_RUN_PIPELINE_TASK = 'projectsRunPipeline'

    final static String TASK_GROUP = 'Saagie'

    static void load(Project project) {
        project.extensions.create('saagie', DataOpsExtension);
        project.ext.SaagieDataOpsExtension = DataOpsExtension;

        project.task(PROJECT_LIST_TASK, type: ProjectListTask) {
            group = TASK_GROUP
            description = 'list all projects on the environment'
            configuration = project.saagie
        }

        project.task(PROJECT_LIST_JOBS_TASK, type: ProjectListJobsTask) {
            group = TASK_GROUP
            description = 'list all jobs of a project'
            configuration = project.saagie
        }

        project.task(PROJECT_LIST_TECHNOLOGIES_TASK, type: ProjectListTechnologiesTask) {
            group = TASK_GROUP
            description = 'list all technologies of a project'
            configuration = project.saagie
        }

        project.task(PROJECT_CREATE_JOB_TASK, type: ProjectCreateJobTask) {
            group = TASK_GROUP
            description = 'create a brand new job in a project'
            configuration = project.saagie
        }

        project.task(PROJECT_UPDATE_JOB_TASK, type: ProjectUpdateJob) {
            group = TASK_GROUP
            description = 'update a existing job in a project'
            configuration = project.saagie
        }

        project.task(PROJECT_RUN_JOB_TASK, type: ProjectRunJobTask) {
            group = TASK_GROUP
            description = 'run an existing job'
            configuration = project.saagie
        }

        project.task(PROJECTS_GET_JOB_INSTANCE_STATUS, type: ProjectGetJobInstanceStatus) {
            group = TASK_GROUP
            description = 'get the status of a job instance'
            configuration = project.saagie
        }

        project.task(PROJECT_CREATE_PIPELINE_TASK, type: ProjectCreatePipelineTask) {
            group = TASK_GROUP
            description = 'create a pipeline'
            configuration = project.saagie
        }

        project.task(PROJECTS_GET_PIPELINE_INSTANCE_STATUS, type: ProjectGetPipelineInstanceStatusTask) {
            group = TASK_GROUP
            description = 'get the status of a pipeline instance'
            configuration = project.saagie
        }

        project.task(PROJECT_UPDATE_PIPELINE_TASK, type: ProjectUpdatePipelineTask) {
            group = TASK_GROUP
            description = 'update a pipeline'
            configuration = project.saagie
        }

        project.task(PROJECT_RUN_PIPELINE_TASK, type: ProjectRunPipelineTask) {
            group = TASK_GROUP
            description = 'run a pipeline'
            configuration = project.saagie
        }
    }
}
