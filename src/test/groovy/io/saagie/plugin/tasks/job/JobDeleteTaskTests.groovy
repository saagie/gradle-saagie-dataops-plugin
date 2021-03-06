package io.saagie.plugin.tasks.job

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_DELETE_JOB_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('projectsDeleteJob task tests')
class JobDeleteTaskTests extends DataOpsGradleTaskSpecification {
    @Shared
    String taskName = PROJECTS_DELETE_JOB_TASK

    def "projectsDeleteJob should delete a job and return the delete status"() {
        given:
        def mockedRunJobResponse = new MockResponse()
        mockedRunJobResponse.responseCode = 200
        mockedRunJobResponse.body = '''{"data":{"archiveJob":true}}'''
        mockWebServer.enqueue(mockedRunJobResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }

                job {
                    id = "jobId"
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

    def "projectsDeleteJob should fail if job id is missing"() {
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
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains("Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/${taskName}")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

    def "projectsDeleteJob should fail if job id doesn't exists"() {
        given:
        def mockedRunJobResponse = new MockResponse()
        mockedRunJobResponse.responseCode = 200
        mockedRunJobResponse.body = '''{"data":{"archiveJob":null},"errors":[{"cause":null,"extensions":{"job":"NOT_EXISTS"},"errorType":"ValidationError","locations":null,"message":"Job not valid","path":null,"localizedMessage":"Job not valid","suppressed":[]}]}'''
        mockWebServer.enqueue(mockedRunJobResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }

                job {
                    id = 'bad-id'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains('Something went wrong when deleting job: {"data":{"archiveJob":null},"errors":[{"cause":null,"extensions":{"job":"NOT_EXISTS"},"errorType":"ValidationError","locations":null,"message":"Job not valid","path":null,"localizedMessage":"Job not valid","suppressed":[]}]}')
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }
}
