package io.saagie.plugin.tasks.pipeline

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_LIST_PIPELINES_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title("projectsListAllPipelinesTask task tests")
class PipelineListAllTaskTests extends DataOpsGradleTaskSpecification {
    @Shared String taskName = PROJECTS_LIST_PIPELINES_TASK

    def "the task should list all projects pipelines"() {
        given:
        def mockedCreatePipelineResponse = new MockResponse()
        mockedCreatePipelineResponse.responseCode = 200
        mockedCreatePipelineResponse.body = '{"data":{"pipelines":[{"id":"pipelineId","name":"Pipeline"}]}}'
        mockWebServer.enqueue(mockedCreatePipelineResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 2
                }

                project {
                    id = 'projectId'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('[{"id":"pipelineId","name":"Pipeline"}]')
    }

    def "the task should fail if the required parameters are missing"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 2
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

    def "the task should list all projects pipelines in jwt mode"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = 'token'
        mockWebServer.enqueue(mockedResponse)

        def mockedCreatePipelineResponse = new MockResponse()
        mockedCreatePipelineResponse.responseCode = 200
        mockedCreatePipelineResponse.body = '{"data":{"pipelines":[{"id":"pipelineId","name":"Pipeline"}]}}'
        mockWebServer.enqueue(mockedCreatePipelineResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 2
                    jwt = true
                }

                project {
                    id = 'projectId'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('[{"id":"pipelineId","name":"Pipeline"}]')
    }
}
