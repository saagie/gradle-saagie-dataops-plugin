package io.saagie.plugin.tasks.technology

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Title("technologyList task tests")
class TechnologyListTests extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    @Shared MockWebServer mockWebServer = new MockWebServer()
    @Shared String taskName = 'technologyList'

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
