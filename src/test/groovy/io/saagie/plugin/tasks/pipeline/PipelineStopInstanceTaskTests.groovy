package io.saagie.plugin.tasks.pipeline

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_STOP_PIPELINE_INSTANCE_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('projectsStopPipelineInstance task tests')
class PipelineStopInstanceTaskTests extends DataOpsGradleTaskSpecification {
    @Shared
    String taskName = PROJECTS_STOP_PIPELINE_INSTANCE_TASK

    def "projectsStopPipelineInstance should stop a pipeline instance"() {
        given:
        def mockedCreatePipelineResponse = new MockResponse()
        mockedCreatePipelineResponse.responseCode = 200
        mockedCreatePipelineResponse.body = '''{"data":{"stopPipelineInstance":{"id":"pipeline-instance-id","status":"KILLED"}}}'''
        mockWebServer.enqueue(mockedCreatePipelineResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'test.user'
                    password = 'password'
                    environment = 2
                }

                pipelineinstance {
                    id = 'pipeline-instance-id'
                }
            }
        """

        when:
        BuildResult result = gradle 'projectsStopPipelineInstance'

        then:
        notThrown(Exception)
        !result.output.contains('{"data"')
        result.output.contains('{"status":"success"}')
    }

    def "projectsStopPipelineInstance should fail if pipeline instance id is missing"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'test.user'
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

    def "projectsStopPipelineInstance should fail if pipeline instance id do not exists"() {
        given:
        def mockedCreatePipelineResponse = new MockResponse()
        mockedCreatePipelineResponse.responseCode = 200
        mockedCreatePipelineResponse.body = '''{"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}'''
        mockWebServer.enqueue(mockedCreatePipelineResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'test.user'
                    password = 'password'
                    environment = 2
                }

                pipelineinstance {
                    id = 'pipeline-instance-id'
                }

            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains('{"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}')
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }
}
