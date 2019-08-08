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

@Title("projectsUpdatePipeline task tests")
class PipelineUpdateTaskTests extends Specification {
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

    def "projectsUpdatePipeline should update pipeline infos"() {
        given:
        def mockedPipelineUpdateResponse = new MockResponse()
        mockedPipelineUpdateResponse.responseCode = 200
        mockedPipelineUpdateResponse.body = '''{"data":{"editPipeline":{"id":"pipeline-id"}}}'''
        mockWebServer.enqueue(mockedPipelineUpdateResponse)

        def mockedPipelineVersionUpdateResponse = new MockResponse()
        mockedPipelineVersionUpdateResponse.responseCode = 200
        mockedPipelineVersionUpdateResponse.body = '''{"data":{"addPipelineVersion":{"number":"pipeline-version-number"}}}'''
        mockWebServer.enqueue(mockedPipelineVersionUpdateResponse)

        buildFile << '''
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.login'
                    password = 'password'
                    environment = 1
                }
                
                pipeline {
                    id = 'pipelineId'
                    name = 'Pipeline updated'
                    description = 'updated description'
                    alerting {
                        emails = ['email@email.com']
                        statusList = ['FAILED']
                    }
                }
                
                pipelineVersion {
                    releaseNote = 'Updated release note'
                    jobs = ['job-id-1', 'job-id-2']
                }
            }
        '''

        when:
        BuildResult result = gradle 'projectsUpdatePipeline'

        then:
        notThrown(Exception)
        result.output.contains('')
    }

    def "projectsUpdatePipeline should fail if no pipeline id is provided"() {
        given:

        buildFile << '''
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.login'
                    password = 'password'
                    environment = 1
                }
                
                pipeline {
                    name = 'Pipeline updated'
                    description = 'updated description'
                    alerting {
                        emails = ['email@email.com']
                        statusList = ['FAILED']
                    }
                }
            }
        '''

        when:
        BuildResult result = gradle 'projectsUpdatePipeline'

        then:
        Exception exception = thrown()
        exception.message.contains('Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/projectsUpdatePipeline')
        result == null
    }
}
