package io.saagie.plugin.dataops

import io.saagie.plugin.dataops.tasks.ProjectCreateJobTask
import io.saagie.plugin.dataops.tasks.ProjectListJobsTask
import io.saagie.plugin.dataops.tasks.ProjectListTask
import io.saagie.plugin.dataops.tasks.ProjectListTechnologiesTask
import org.gradle.api.Project

class DataOpsModule {
    static void load(Project project) {
        project.extensions.create('saagie', DataOpsExtension);
        project.ext.SaagieDataOpsExtension = DataOpsExtension;

        project.task('projectList', type: ProjectListTask) {
            group = 'Saagie'
            configuration = project.saagie
        }

        project.task('projectListJobs', type: ProjectListJobsTask) {
            group = 'Saagie'
            configuration = project.saagie
        }

        project.task('projectListTechnologies', type: ProjectListTechnologiesTask) {
            group: 'Saagie'
            configuration = project.saagie
        }

        project.task('projectsCreateJob', type: ProjectCreateJobTask) {
            group: 'Saagie'
            configuration = project.saagie
        }
    }
}
