package io.saagie.plugin.job

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('projectsListJob task tests')
class JobListTaskTests extends Specification {
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

    def "projectsListJobs task should list jobs on a project"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """{"data":{"jobs":[{"name":"test2","description":"","countJobInstance":1,"versions":[{"number":1}],"category":"Processing","technology":{"id":"frefref-c18b-4ecd-b61f-frefefreff","label":"Python","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-03-15T14:06:49.053Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":true},{"name":"test 2","description":"","countJobInstance":4,"versions":[{"number":2},{"number":0}],"category":"Processing","technology":{"id":"dezded-26bd-4f7d-a3a5-dezdedzdz","label":"Spark","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-03-11T09:32:46.424Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":true}]}}"""
        buildFile << '''
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }
                
                project {
                    id = 'projectId'
                }
            }
        '''
        mockWebServer.enqueue(mockedResponse)

        when:
        def result = gradle 'projectsListJobs'

        then:
        !result.output.contains('"data"')
        result.output.contains('"name"')
        result.output.contains('"id"')
        result.output.contains('"countJobInstance"')
    }

    def "projectsListJobs task should fail if bad project config is provided"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }
                
                project {
                
                }
            }
        """

        when:
        gradle 'projectsListJobs'

        then:
        UnexpectedBuildFailure e = thrown()
        e.message.contains('Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/projectsListJobs')
        e.getBuildResult().task(':projectsListJobs').outcome == FAILED
    }

    def "projectsListJobs task should fail if a wrong project id is provided"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """{"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}"""

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }
                
                project {
                    id = 'wrong id'
                }
            }
        """
        mockWebServer.enqueue(mockedResponse)

        when:
        gradle 'projectsListJobs'

        then:
        UnexpectedBuildFailure e = thrown()
        e.message.contains('Something went wrong when getting project jobs: ')
        e.getBuildResult().task(':projectsListJobs').outcome == FAILED
    }
}
