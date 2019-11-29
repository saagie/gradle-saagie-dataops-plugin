package io.saagie.plugin.tasks.project

import io.saagie.plugin.DataOpsGradleTaskSpecification
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.IgnoreRest
import spock.lang.Shared

import static org.gradle.testkit.runner.TaskOutcome.FAILED

class ProjectUpdateTask extends DataOpsGradleTaskSpecification {
    @Shared String taskName = 'projectsUpdate'

    def "the task should update the project with only required params"() {
        given:
        enqueueRequest('{"data":{"editProject":{"id":"project-id","name":"project name","description":"project description","status":"READY"}}}')
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 1
                }

                project {
                    id = 'project-id'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{"id":"project-id","name":"project name","description":"project description","status":"READY"}')
    }

    def "the task should work when all params are provided"() {
        given:
        enqueueRequest('{"data":{"editProject":{"id":"project-id","name":"project name","description":"project description","status":"READY"}}}')
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 1
                }

                project {
                    id = 'project-id'
                    description = 'new description'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{"id":"project-id","name":"project name","description":"project description","status":"READY"}')
    }

    def "the task should fail if required params are not provided"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 1
                }

                project {
                    description = 'new description'
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
