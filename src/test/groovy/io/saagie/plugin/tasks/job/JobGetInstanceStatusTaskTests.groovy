package io.saagie.plugin.tasks.job

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_GET_JOB_INSTANCE_STATUS

@Title('projectsGetJobInstanceStatus task tests')
class JobGetInstanceStatusTaskTests extends DataOpsGradleTaskSpecification {
    @Shared
    String taskName = PROJECTS_GET_JOB_INSTANCE_STATUS;

    def "projectsGetJobInstanceStatus should return the status of the jobInstance"() {
        given:
        def mockedRunJobResponse = new MockResponse()
        mockedRunJobResponse.responseCode = 200
        mockedRunJobResponse.body = '''{"data":{"jobInstance":{"status":"SUCCEEDED"}}}'''
        mockWebServer.enqueue(mockedRunJobResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }

                jobinstance {
                    id = "jobInstanceId"
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        !result.output.contains('"data"')
        result.output.contains('"status"')
    }

    def "projectsGetJobInstanceStatus should fail if no jobInstance id was provided"() {
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
        thrown(Exception)
        result == null
    }

}
