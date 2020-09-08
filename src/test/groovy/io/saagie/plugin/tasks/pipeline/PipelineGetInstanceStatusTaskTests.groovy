package io.saagie.plugin.tasks.pipeline

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildResultException
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_GET_PIPELINE_INSTANCE_STATUS
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('projectsGetPipelineInstanceStatus task tests')
class PipelineGetInstanceStatusTaskTests extends DataOpsGradleTaskSpecification {
    @Shared
    String taskName = PROJECTS_GET_PIPELINE_INSTANCE_STATUS

    def "projectsGetPipelineInstanceStatus should return the status of the pipelineInstance"() {
        given:
        def mockedRunJobResponse = new MockResponse()
        mockedRunJobResponse.responseCode = 200
        mockedRunJobResponse.body = '''{"data":{"pipelineInstance":{"status":"SUCCEEDED"}}}'''
        mockWebServer.enqueue(mockedRunJobResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }

                pipelineinstance {
                    id = "pipelineInstanceId"
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        !result.output.contains('"data"')
        result.output.contains('"status"')
    }

    def "projectsGetPipelineInstanceStatus should fail if no pipelineInstance id was provided"() {
        given:

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildResultException e = thrown()
        result == null
        e.message.contains("Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/${taskName}")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }
}
