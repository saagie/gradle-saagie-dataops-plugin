package io.saagie.plugin.pipeline

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Title('projectsCreatePipelineJob task tests')
class PipelineCreateTaskTests extends Specification {
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

    def "projectsCreatePipelineJob should create a new pipeline"() {
        given:
        def mockedCreatePipelineResponse = new MockResponse()
        mockedCreatePipelineResponse.responseCode = 200
        mockedCreatePipelineResponse.body = '''{"data":{"createPipeline":{"id":"pipeline-instance-id"}}}'''
        mockWebServer.enqueue(mockedCreatePipelineResponse)

        buildFile << """
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
                
                pipeline {
                    name = 'Pipeline name'
                }
                
                pipelineVersion {
                    releaseNote = 'Release note'
                    jobs = ['jobId-1', 'jobId-2']
                }
            }
        """

        when:
        BuildResult result = gradle 'projectsCreatePipeline'

        then:
        result.output.contains('{"id":"pipeline-instance-id"}')
    }

    def "projectsCreatePipelineJob should fail if required config is missing"() {
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
                    id = 'projectId'
                }
            }
        """

        when:
        BuildResult result = gradle 'projectsCreatePipeline'

        then:
        thrown(Exception)
        result == null
    }

    def "projectsCreatePipelineJob should fail if there is already a pipeline with the same name"() {
        given:
        def mockedCreatePipelineResponse = new MockResponse()
        mockedCreatePipelineResponse.responseCode = 200
        mockedCreatePipelineResponse.body = '''{"data":null,"errors":[{"cause":null,"extensions":{"name":"already used"},"locations":null,"errorType":"ValidationError","message":"Pipeline not valid","path":null,"localizedMessage":"Pipeline not valid","suppressed":[]}]}'''
        mockWebServer.enqueue(mockedCreatePipelineResponse)

        buildFile << """
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
                
                pipeline {
                    name = 'Pipeline name already used'
                }
                
                pipelineVersion {
                    releaseNote = 'Release note'
                    jobs = ['jobId-1', 'jobId-2']
                }
            }
        """

        when:
        BuildResult result = gradle 'projectsCreatePipeline'

        then:
        Exception e = thrown()
        e.message.contains('{"data":null,"errors":[{"cause":null,"extensions":{"name":"already used"},"locations":null,"errorType":"ValidationError","message":"Pipeline not valid","path":null,"localizedMessage":"Pipeline not valid","suppressed":[]}]}')
    }

}
