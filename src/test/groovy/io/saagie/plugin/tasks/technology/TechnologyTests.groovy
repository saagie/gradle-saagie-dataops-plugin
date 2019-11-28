package io.saagie.plugin.tasks.technology

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECT_LIST_TECHNOLOGIES_TASK

@Title("projectsListTechnologies task tests")
class TechnologyTests extends DataOpsGradleTaskSpecification {
    @Shared String taskName = PROJECT_LIST_TECHNOLOGIES_TASK

    def "projectsListTechnologies task should list technologies of a project"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """{"data":{"technologies":[{"id":"c3cadcad-fjrehf-4f7d-a3a5-frefer","label":"Spark","isAvailable":true,"icon":"spark","features":[]},{"id":"freojfier-c18b-4ecd-b61f-fjerijfiej","label":"Python","isAvailable":true,"icon":"python","features":[]},{"id":"fkiorjeiofer-c18b-4ecd-b61f-jkfijorjferferf","label":"Python","isAvailable":true,"icon":"python","features":[]},{"id":"frefreferfe-26bd-4f7d-a3a5-frejferiuh","label":"Spark","isAvailable":true,"icon":"spark","features":[]}]}}"""
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }

                project {
                    id = 'dezdezjiodjei-892a-2342-8552-5be4b6de5df4'
                }
            }
        """
        mockWebServer.enqueue(mockedResponse)

        when:
        BuildResult result = gradle(taskName)

        then:
        !result.output.contains('"data"')
        result.output.contains('"label"')
        result.output.contains('"features"')
    }

    def "listed technologies should not show duplicated entries"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """{"data":{"technologies":[{"id":"python-id","label":"Python","isAvailable":true,"icon":"python","features":[]},{"id":"bash-id","label":"Bash","isAvailable":true,"icon":"bash","features":[]},{"id":"bash-id","label":"Bash","isAvailable":true,"icon":"bash","features":[]}]}}"""
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }

                project {
                    id = 'dezdezjiodjei-892a-2342-8552-5be4b6de5df4'
                }
            }
        """
        mockWebServer.enqueue(mockedResponse)

        when:
        BuildResult result = gradle(taskName)

        then:
        result.output.count('bash-id') == 1
        result.output.count('python-id') == 1
    }
}
