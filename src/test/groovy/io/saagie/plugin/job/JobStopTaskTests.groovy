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

@Title('projectsStopJobInstance task tests')
class JobStopTaskTests extends Specification {
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

    def "projectsStopJobInstance should stop a job"() {
        given:
        def mockedStopJobResponse = new MockResponse()
        mockedStopJobResponse.responseCode = 200
        mockedStopJobResponse.body = '''{"data":{"stopJobInstance":{"id":"stopped-job-id"}}}'''
        mockWebServer.enqueue(mockedStopJobResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.user'
                    password = 'password'
                    environment = 4
                }
                
                jobInstance {
                    id = 'job-instance-id'
                }
            }
        """

        when:
        BuildResult result = gradle 'projectsStopJobInstance'

        then:
        result.output.contains('{"id":"stopped-job-id"}')
    }

    def "projectsStopJobInstance should fail if job instance id is missing"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.user'
                    password = 'password'
                    environment = 4
                }
            }
        """

        when:
        BuildResult result = gradle 'projectsStopJobInstance'

        then:
        Exception e = thrown()
        result == null
        e.message.contains("Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/projectsStopJobInstance")
    }

    def "projectsStopJobInstance should fail if job instance doesn't exists"() {
        given:
        def mockedRunJobResponse = new MockResponse()
        mockedRunJobResponse.responseCode = 200
        mockedRunJobResponse.body = '''{"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}'''
        mockWebServer.enqueue(mockedRunJobResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'test.user'
                    password = 'password'
                    environment = 2
                }
                
                jobInstance {
                    id = 'bad-id'
                }
            }
        """

        when:
        BuildResult result = gradle 'projectsStopJobInstance'

        then:
        Exception e = thrown()
        result == null
        e.message.contains('Something went wrong when stopping the job instance: {"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}')
    }
}
