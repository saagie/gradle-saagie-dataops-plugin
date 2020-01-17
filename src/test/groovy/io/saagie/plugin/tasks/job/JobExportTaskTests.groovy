package io.saagie.plugin.tasks.job

import io.saagie.plugin.DataOpsGradleTaskSpecification
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_EXPORT_JOB;
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('JobExportTaskTests task tests')
class JobExportTaskTests extends DataOpsGradleTaskSpecification {
    @Shared String taskName = PROJECTS_EXPORT_JOB

    def "projectsExportJob should export job and job version"() {

        given:
        buildFile << """
            saagie {
                server {
                    url = 'https://saagie.url'
                    login = 'username'
                    password = 'password'
                    environment = 0
                    useLegacy = false
                }

                project {
                    id = 'project-id'
                }

                job {
                    id = 'job-id'
                }

                export {
                    export_file_path = 'export_file'
                    overwrite = true 
                }
            }
        """

        when:
        BuildResult result = gradle(taskName, "-d")
        then:
        notThrown(Exception)
        assert new File('/export_file/').exists()
    }

}
