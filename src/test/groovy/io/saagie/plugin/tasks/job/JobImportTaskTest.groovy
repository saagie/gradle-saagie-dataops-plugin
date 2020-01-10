package io.saagie.plugin.tasks.job

import io.saagie.plugin.DataOpsGradleTaskSpecification
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_IMPORT_JOB
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('projectsImportJob task tests')
class JobImportTaskTest extends DataOpsGradleTaskSpecification {
    @Shared String taskName = PROJECTS_IMPORT_JOB

    def "the task should fail if required params are not provided"() {
        given:
        buildFile << '''
            saagie {
                server {
                    url = 'https://localhost:9000'
                    login = 'login'
                    password = 'password'
                    environment = 1
                }
            }
        '''

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains("Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/${taskName}")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

    def "the task should create a new job based on the exported config"() {
        given:
        buildFile << '''
            saagie {
                server {
                    url = 'https://localhost:9000'
                    login = 'login'
                    password = 'password'
                    environment = 1
                }

                project {
                    id = 'project-id'
                }

                importJob {
                    import_file = ''
                }
            }
        '''

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
    }
}
