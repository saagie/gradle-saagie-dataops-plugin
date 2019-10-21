package io.saagie.plugin

import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.*

@Title("Plugin integration test with gradle")
class DataOpsPluginTest extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()
    @Shared
    MockWebServer mockWebServer = new MockWebServer()

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

    def "./gradlew tasks should show all tasks under a Saagie group"() {
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
        BuildResult result = gradle 'tasks', '--all'

        then:
        notThrown()
        result.output.contains 'Saagie tasks'
        result.output.contains 'projectsCreateJob - create a brand new job in a project'
        result.output.contains 'projectsCreatePipeline - create a pipeline'
        result.output.contains 'projectsGetJobInstanceStatus - get the status of a job instance'
        result.output.contains 'projectsGetPipelineInstanceStatus - get the status of a pipeline instance'
        result.output.contains 'projectsList - list all projects on the environment'
        result.output.contains 'projectsListJobs - list all jobs of a project'
        result.output.contains 'projectsListTechnologies - list all technologies of a project'
        result.output.contains 'projectsRunJob - run an existing job'
        result.output.contains 'projectsRunPipeline - run a pipeline'
        result.output.contains 'projectsStopPipelineInstance - stop a pipeline instance'
        result.output.contains 'projectsUpdatePipeline - update a pipeline'
        result.output.contains 'projectsUpdateJob - update a existing job in a project'
        result.output.contains 'platformList - list available platforms'
    }
}
