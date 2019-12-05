package io.saagie.plugin.tasks.project

import io.saagie.plugin.DataOpsGradleTaskSpecification

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECT_DELETE_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title("projectsDelete task tests")
class ProjectDeleteTaskTests extends DataOpsGradleTaskSpecification {

    @Shared String taskName = PROJECT_DELETE_TASK

    def "projectsDelete should delete a project the archive status"() {
        given:

        enqueueRequest('''{"data":{"archiveProject": true}}''')

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'test.user'
                    password = 'password'
                    environment = 1
                }

                project {
                    id = "project-id"
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        !result.output.contains('"data"')
        result.output.contains('{"status":"success"}')
    }

    def "projectsDelete should fail if project id doesn't exists"() {
        given:
        enqueueRequest('''{"errors":[{"message":"Unexpected error"}],"data":null}''')

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.test'
                    password = 'password'
                    environment = 1
                }

                project {
                    id = 'bad-id'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains('Something went wrong when deleting project: {"errors":[{"message":"Unexpected error"}],"data":null}')
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

    def "projectsDelete should fail if no project id is provided"() {
        given:

        enqueueRequest('''{"errors":[{"message":"Unexpected error"}],"data":null}''')

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.test'
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

}
