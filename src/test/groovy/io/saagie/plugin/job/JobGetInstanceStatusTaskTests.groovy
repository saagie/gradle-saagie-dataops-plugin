package io.saagie.plugin.job

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Title('projectsGetJobInstanceStatus task tests')
class JobGetInstanceStatusTaskTests extends Specification {
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

                jobInstance {
                    id = "jobInstanceId"
                }
            }
        """

        when:
        BuildResult result = gradle 'projectsGetJobInstanceStatus'

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
        BuildResult result = gradle 'projectsGetJobInstanceStatus'

        then:
        thrown(Exception)
        result == null
    }

}
