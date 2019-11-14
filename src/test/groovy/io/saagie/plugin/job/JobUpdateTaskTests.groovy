package io.saagie.plugin.job

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.clients.SaagieClient
import io.saagie.plugin.dataops.models.JobVersion
import io.saagie.plugin.dataops.utils.SaagieUtils
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

@Title('projectsUpdateJob task tests')
class JobUpdateTaskTests extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    @Shared MockWebServer mockWebServer = new MockWebServer()
    @Shared String taskName = 'projectsUpdateJob'

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

    def "projectsUpdateJob should update the specified job with only job config"() {
        given:
        def mockedJobCreationResponse = new MockResponse()
        mockedJobCreationResponse.responseCode = 200
        mockedJobCreationResponse.body = '''{"data":{"editJob":{"id":"jobId"}}}'''
        mockWebServer.enqueue(mockedJobCreationResponse)

        buildFile << '''
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.user'
                    password = 'password'
                    environment = 2
                }

                job {
                    id = 'jobId'
                    name = 'Updated from gradle'
                    description = 'updated description'
                    alerting {
                        emails = ['email@email.com']
                        statusList = ['REQUESTED']
                    }
                }
            }
        '''

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{"id":"jobId"}')
    }

    def "projectsUpdateJob should fail if job id is missing"() {
        given:
        buildFile << '''
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.user'
                    password = 'password'
                    environment = 2
                }

                job {
                    name = 'Updated from gradle'
                    description = 'updated description'
                    alerting {
                        emails = ['email@email.com']
                        statusList = ['REQUESTED']
                    }
                }
            }
        '''

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains("Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/${taskName}")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

    def "projectsUpdateJob should add a new job version and upload script if config is provided"() {
        given:
        def mockedJobUpdateResponse = new MockResponse()
        mockedJobUpdateResponse.responseCode = 200
        mockedJobUpdateResponse.body = '''{"data":{"editJob":{"id":"jobId"}}}'''
        mockWebServer.enqueue(mockedJobUpdateResponse)

        def mockedJobVersionResponse = new MockResponse()
        mockedJobVersionResponse.responseCode = 200
        mockedJobVersionResponse.body = '''{"data":{"addJobVersion":{"number":"jobNumber"}}}'''
        mockWebServer.enqueue(mockedJobVersionResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.user'
                    password = 'password'
                    environment = 4
                }

                job {
                    id = 'jobId'
                }

                jobVersion {
                    commandLine = "python {file}"
                    releaseNote = "Feat: won't fail"
                    runtimeVersion = "3.6"
                    packageInfo {
                        name = "${jobFile.absolutePath}"
                    }
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        result.output.contains('"id"')
    }

    def "JobVersion object DSL should return null values on empty array"() {
        given:
        JobVersion jobVersion = new JobVersion()
        jobVersion.with {
            runtimeVersion = '1'
            releaseNote = 'release note'
            volume = []
            exposedPorts = []
        }

        when:
        Map jobVersionMap = jobVersion.toMap()

        then:
        jobVersionMap.volume == null
        jobVersionMap.exposedPorts == null
    }

    def "projectsUpdateJob should fail if jobVersion is provided without a runtimeVersion"() {
        given: "Build file without jobVersion.runtimeVersion"
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.user'
                    password = 'password'
                    environment = 4
                }

                job {
                    id = 'jobId'
                }

                jobVersion {
                    releaseNote = 'test release note'
                }
            }
        """

        when: "gradle ${taskName}"
        BuildResult result = gradle(taskName)

        then: "Expect an error to be thrown, and a link to the corresponding task doc"
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains("Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/${taskName}")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

}
