package io.saagie.plugin.tasks.job

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECT_UPDATE_JOB_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('projectsUpdateJob task tests')
class JobUpdateTaskTests extends DataOpsGradleTaskSpecification {
    @Shared String taskName = PROJECT_UPDATE_JOB_TASK

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

        jobFile << 'println("Hello gradle")'
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
        result.output.contains('{"id":"jobId"}')
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
