package io.saagie.plugin.pipeline

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

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_LIST_ALL_PIPELINES
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title("projectsListAllPipelinesTask task tests")
class PipelineListAllTaskTests extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    @Shared MockWebServer mockWebServer = new MockWebServer()
    @Shared String taskName = PROJECTS_LIST_ALL_PIPELINES

    File buildFile

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

    def "the task should list all projects pipelines"() {
        given:
        def mockedCreatePipelineResponse = new MockResponse()
        mockedCreatePipelineResponse.responseCode = 200
        mockedCreatePipelineResponse.body = '{"data":{"pipelines":[{"id":"pipelineId","name":"Pipeline"}]}}'
        mockWebServer.enqueue(mockedCreatePipelineResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 2
                }

                project {
                    id = 'projectId'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('[{"id":"pipelineId","name":"Pipeline"}]')
    }

    def "the task should fail if the required parameters are missing"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 2
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

    def "the task should list all projects pipelines in jwt mode"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = 'token'
        mockWebServer.enqueue(mockedResponse)

        def mockedCreatePipelineResponse = new MockResponse()
        mockedCreatePipelineResponse.responseCode = 200
        mockedCreatePipelineResponse.body = '{"data":{"pipelines":[{"id":"pipelineId","name":"Pipeline"}]}}'
        mockWebServer.enqueue(mockedCreatePipelineResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 2
                    jwt = true
                }

                project {
                    id = 'projectId'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('[{"id":"pipelineId","name":"Pipeline"}]')
    }
}
