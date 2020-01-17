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

    def "the task should fail if required params are not provided"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = '${mockServerUrl}'
                    login = 'login'
                    password = 'password'
                    environment = 1
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains("Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/${taskName}")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

    def "projectsExportJob should export job and job version"() {

        File tempJobDirectory = File.createTempDir("project", ".tmp");
        given:
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000/'
                    login = 'user'
                    password = 'password'
                    environment = 1
                    useLegacy = false
                }

                project {
                    id = 'project-id'
                }

                job {
                    id = 'job-id'
                }

                export {
                    export_file_path = '${tempJobDirectory.getAbsolutePath()}'
                    overwrite = true 
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)
        then:
        notThrown(Exception)
        assert new File("${tempJobDirectory.getAbsolutePath()}/projet-projet-id.zip").exists()

    }

    def "the task should fail if the export_file_path does not exists"() {
        given:
        buildFile << """
             saagie {
                server {
                    url = 'http://localhost:9000/'
                    login = 'user'
                    password = 'password'
                    environment = 1
                    useLegacy = false
                }

                project {
                    id = 'project-id'
                }

                job {
                    id = 'job-id'
                }

                export {
                    export_file_path = 'invalide/path/directory'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains("configuration export path does not exist")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

}
