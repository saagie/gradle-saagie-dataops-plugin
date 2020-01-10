package io.saagie.plugin.tasks.job

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
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
                    url = 'https://saagie.io/'
                    login = 'username'
                    password = 'password'
                    environment = 4
                    useLegacy = false
                }

                project {
                    id = 'projet-id'
                }

                job {
                    id = 'job-id'
                }

                export {
                    export_file_path = '/home/saagie/export/'
                    overwrite = true 
                }
            }
        """

        when:
        BuildResult result = gradle(taskName, "-d")
        then:
        notThrown(Exception)
        result != null
    }

}
