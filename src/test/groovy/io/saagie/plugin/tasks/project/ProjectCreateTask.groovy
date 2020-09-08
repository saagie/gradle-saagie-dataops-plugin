package io.saagie.plugin.tasks.project

import io.saagie.plugin.DataOpsGradleTaskSpecification
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_CREATE_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title("projectsList task tests")
class ProjectCreateTask extends DataOpsGradleTaskSpecification {
    @Shared
    String taskName = PROJECTS_CREATE_TASK

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

    def "the task should create a new project when all params are given"() {
        given:
        enqueueRequest('{"data":{"createProject":{"id":"project-id","name":"project","description":"description"}}}')

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
                    description = 'description'
                    technologyByCategory = [
                        {
                            category = 'Extraction'
                            technologyid = ['tech-id']
                        }
                    ]
                    authorizedGroups = [
                        {
                            id = 'users'
                            role = 'ROLE_PROJECT_VIEWER'
                        }
                    ]
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{"id":"project-id","name":"project","description":"description"}')
    }

    def "the task should fail if required params are missing"() {
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
                    description = 'description'
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
