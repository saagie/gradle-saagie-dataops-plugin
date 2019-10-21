package io.saagie.plugin.job

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

@Title('projectsCreateJob task tests')
class JobCreateTaskTests extends Specification {
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
        jobFile << """print('Hello World')"""
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

    def "projectsCreateJob should create job and upload a file for a given project"() {
        given:
        def mockedJobCreationResponse = new MockResponse()
        mockedJobCreationResponse.responseCode = 200
        mockedJobCreationResponse.body = '''{"data":{"createJob":{"id":"kdiojezidz-ce2a-486e-b524-d40ff353eea7"}}}'''
        mockWebServer.enqueue(mockedJobCreationResponse)

        def mockedFileUploadResponse = new MockResponse()
        mockedFileUploadResponse.responseCode = 200
        mockedFileUploadResponse.body = '''true'''
        mockWebServer.enqueue(mockedFileUploadResponse)

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

                job {
                    name = "My custom job"
                    category = "Extraction"
                    technology = "technologyId"
                }

                jobVersion {
                    runtimeVersion = "3.6"
                    commandLine = "python {file} arg1 arg2"
                    releaseNote = "First job version"
                    packageInfo {
                        name = "${jobFile.absolutePath}"
                    }
                }
            }
        """

        when:
        def result = gradle 'projectsCreateJob'

        then:
        !result.output.contains('"data"')
        result.output.contains('"id"')
    }

    def "projectsCreateJob should fail if the file to upload doesn't exists"() {
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

                job {
                    name = "My custom job"
                    category = "Extraction"
                    technology = "technologyId"
                }

                jobVersion {
                    runtimeVersion = "3.6"
                    commandLine = "python {file} arg1 arg2"
                    releaseNote = "First job version"
                    packageInfo {
                        name = "bad/path/to-file.sh"
                    }
                }
            }
        """

        when:
        BuildResult result = gradle 'projectsCreateJob'

        then:
        thrown(Exception)
        result == null
    }

    def "projectsCreateJob should fail if the job name is already taken"() {
        given:
        def mockedJobCreationResponse = new MockResponse()
        mockedJobCreationResponse.responseCode = 200
        mockedJobCreationResponse.body = '''{"errors":[{"message":"Job not valid","extensions":{"name":"already used","classification":"ValidationError"}}],"data":null}'''
        mockWebServer.enqueue(mockedJobCreationResponse)

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

                job {
                    name = "My custom job"
                    category = "Extraction"
                    technology = "technologyId"
                }

                jobVersion {
                    runtimeVersion = "3.6"
                    commandLine = "python {file} arg1 arg2"
                    releaseNote = "First job version"
                    packageInfo {
                        name = "${jobFile.absolutePath}"
                    }
                }
            }
        """

        when:
        BuildResult result = gradle ('projectsCreateJob')

        then:
        Exception e = thrown()
        result == null
        e.message.contains('''{"errors":[{"message":"Job not valid","extensions":{"name":"already used","classification":"ValidationError"}}],"data":null}''')
    }
}
