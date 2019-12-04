package io.saagie.plugin.tasks.technology

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.TECHNOLOGY_LIST_TASK

@Title("technologyList task tests")
class TechnologyListTests extends DataOpsGradleTaskSpecification {
    @Shared String taskName = TECHNOLOGY_LIST_TASK

    def "the task should return a list of all technologies"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = '{"data":{"technologies":[{"id":"39510510-7262-459d-83c2-8e5d2e8bbdd8","label":"Docker","isAvailable":true}]}}'
        mockWebServer.enqueue(mockedResponse)

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
        notThrown(Exception)
        result.output.contains('[{"id":"39510510-7262-459d-83c2-8e5d2e8bbdd8","label":"Docker","isAvailable":true}]')
    }

    def "the task should be working in jwt mode"() {
        given:
        def tokenResponse = new MockResponse()
        tokenResponse.responseCode = 200
        tokenResponse.body = 'token'
        mockWebServer.enqueue(tokenResponse)

        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = '{"data":{"technologies":[{"id":"39510510-7262-459d-83c2-8e5d2e8bbdd8","label":"Docker","isAvailable":true}]}}'
        mockWebServer.enqueue(mockedResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                    jwt = true
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('[{"id":"39510510-7262-459d-83c2-8e5d2e8bbdd8","label":"Docker","isAvailable":true}]')
    }
}
