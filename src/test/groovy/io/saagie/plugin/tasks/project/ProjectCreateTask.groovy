package io.saagie.plugin.tasks.project

import io.saagie.plugin.DataOpsGradleTaskSpecification
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.PendingFeature
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_CREATE_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title("projectsList task tests")
class ProjectCreateTask extends DataOpsGradleTaskSpecification {
    @Shared String taskName = PROJECTS_CREATE_TASK

    def "the task should create a new project with only required params"() {
        given:
        enqueueRequest('{"data":{"createProject":{"id":"project-id","name":"project","description":""}}}')

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 1
                }

                project {
                    name = 'My project'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{"id":"project-id","name":"project","description":""}')
    }

    @PendingFeature
    def "the task should create a new project when all params are given"() {

    }

    @PendingFeature
    def "the task should fail if required params are missing"() {

    }

    def "the task should fail if the required name is already used"() {
        given:
        enqueueRequest('{"errors":[{"message":"Project not valid","extensions":{"name":"already used","classification":"ValidationError"}}],"data":null}')
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 1
                }

                project {
                    name = 'duplicated name'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.getMessage().contains('Something went wrong when creating project: {"errors":[{"message":"Project not valid","extensions":{"name":"already used","classification":"ValidationError"}}],"data":null}')
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }
}
