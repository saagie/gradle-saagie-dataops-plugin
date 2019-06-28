package io.saagie.plugin.dataops

import io.saagie.plugin.dataops.tasks.ProjectListTask
import org.gradle.api.Project

class DataOpsModule {
    static void load(Project project) {
        project.extensions.create('saagie', DataOpsExtension);
        project.ext.DataOpsExtension = DataOpsExtension;

        project.task('hello') {
            group = "Saagie"
            description = "Greets the world. Greeting configured in the 'saagie' extension."

            doLast {
                String greeting = project.extensions.saagie.alternativeGreeting ?: "Hello"
                println "$greeting, world!"
            }
        }

        project.task('projectList', type: ProjectListTask) {
            group = "Saagie"
            configuration = project.saagie
        }
    }
}
