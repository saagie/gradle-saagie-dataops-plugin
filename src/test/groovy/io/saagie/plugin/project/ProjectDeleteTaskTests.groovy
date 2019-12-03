package io.saagie.plugin.project

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

@Title("projectsDelete task tests")
class ProjectDeleteTaskTests extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    @Shared MockWebServer mockWebServer = new MockWebServer()
    @Shared String taskName = 'projectsDelete'

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

    def "projectsDelete should fail if project id doesn't exists"() {
        given:
        def mockedDeleteProjectResponse = new MockResponse()
        mockedDeleteProjectResponse.responseCode = 200
        mockedDeleteProjectResponse.body = '''{"errors":[{"message":"Unexpected error"}],"data":null}'''
        mockWebServer.enqueue(mockedDeleteProjectResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.test'
                    password = 'password'
                    environment = 1
                }

                project {
                    id = 'bad-id'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains('Something went wrong when deleting project: {"errors":[{"message":"Unexpected error"}],"data":null}')
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

    def "projectsDelete should fail if no project id is provided"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.test'
                    password = 'password'
                    environment = 1
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

    def "projectsDelete should delete a project the archive status"() {
        given:
        def mockedDeleteResponse = new MockResponse()
        mockedDeleteResponse.responseCode = 200
        mockedDeleteResponse.body = '''{"data":{"archiveProject":true}}'''
        mockWebServer.enqueue(mockedDeleteResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.test'
                    password = 'password'
                    environment = 1
                }

                project {
                    id = "c1ff8e71-2ee9-4016-8ea9-de1b3f2fecb9"
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        !result.output.contains('"data"')
        result.output.contains('{"status":"success"}')
    }
}
