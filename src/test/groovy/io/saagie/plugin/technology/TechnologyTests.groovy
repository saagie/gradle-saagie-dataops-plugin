package io.saagie.plugin.technology

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Title("projectsListTechnologies task tests")
class TechnologyTests extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    @Shared MockWebServer mockWebServer = new MockWebServer()

    File buildFile
    File jobFile

    def setupSpec() {
        mockWebServer.start(9000)
    }

    def cleanupSpec() {
        mockWebServer.shutdown()
    }

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << 'plugins { id "io.saagie.gradle-saagie-dataops-plugin" }\n'

        jobFile = testProjectDir.newFile('jobFile.py')
    }

    def cleanup() {
        mockWebServer.dispatcher.peek()
    }

    private BuildResult gradle(boolean isSuccessExpected, String[] arguments = ['tasks']) {
        arguments += '--stacktrace'
        def runner = GradleRunner.create()
            .withArguments(arguments)
            .withProjectDir(testProjectDir.root)
            .withPluginClasspath()
            .withDebug(true)

        return isSuccessExpected ? runner.build() : runner.buildAndFail();
    }

    private BuildResult gradle(String[] arguments = ['tasks']) {
        gradle(true, arguments)
    }

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
        def result = gradle 'projectsListTechnologies'

        then:
        !result.output.contains('"data"')
        result.output.contains('"label"')
        result.output.contains('"features"')
    }
}
