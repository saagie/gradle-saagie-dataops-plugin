package io.saagie.plugin.tasks.pipeline

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_RUN_PIPELINE_TASK
import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_STOP_PIPELINE_INSTANCE_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('projectsRunPipelineInstance task tests')
class PipelineRunTaskTests extends DataOpsGradleTaskSpecification {
    @Shared String taskName = PROJECTS_RUN_PIPELINE_TASK

    def "projectsRunPipelineInstance should run a pipeline instance"() {
        given:
        def mockedCreatePipelineResponse = new MockResponse()
        mockedCreatePipelineResponse.responseCode = 200
        mockedCreatePipelineResponse.body = '''{"data":{"runPipeline":{"id":"pipeline-instance-id"}}}'''
        mockWebServer.enqueue(mockedCreatePipelineResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'test.user'
                    password = 'password'
                    environment = 2
                }

                pipeline {
                    id = 'pipeline-instance-id'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        !result.output.contains('{"data"')
        result.output.contains('{"runPipeline":{"id":"pipeline-instance-id"}}')
    }

    def "projectsRunPipelineInstance should fail if pipeline id is missing"() {
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

    def "projectsStopPipelineInstance should fail if pipeline id do not exists"() {
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

                pipeline {
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
