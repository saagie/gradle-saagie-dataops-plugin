package io.saagie.plugin.tasks.project

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_LIST_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title("projectsList task tests")
class ProjectListTaskTests extends DataOpsGradleTaskSpecification {
    String taskName = PROJECTS_LIST_TASK

    def "projectsList task should return a list of project"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """{"data":{"projects":[{"id":"8321e13c-892a-4481-8552-dekzdjeijzd","name":"Test new Project"},{"id":"7f5e0374-0c45-45a3-a2f3-dkjezoijdizd","name":"Test Spark config"},{"id":"bba3511b-7b7f-44f0-9f69-djeizjdoijzj","name":"For tests"},{"id":"9feae78d-1cc0-49bd-9e63-deozjiodjeiz","name":"Test simon"}]}}"""

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
        mockWebServer.enqueue(mockedResponse)

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        !result.getOutput().contains('"data"')
        result.getOutput().contains('[{"id":"8321e13c-892a-4481-8552-dekzdjeijzd","name":"Test new Project"},{"id":"7f5e0374-0c45-45a3-a2f3-dkjezoijdizd","name":"Test Spark config"},{"id":"bba3511b-7b7f-44f0-9f69-djeizjdoijzj","name":"For tests"},{"id":"9feae78d-1cc0-49bd-9e63-deozjiodjeiz","name":"Test simon"}]')
    }

    def "projectsList task with bad config should fail"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 400
        mockWebServer.enqueue(mockedResponse)

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
        e.getMessage().contains('Error 400 when requesting')
        e.getBuildResult().task(':projectsList').outcome == FAILED
    }

    def "projectsList task should print additional infos in info mode"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """{"data":{"projects":[{"id":"projectId","name":"Test new Project"},{"id":"projectId2","name":"Test Spark config"}]}}"""

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
        mockWebServer.enqueue(mockedResponse)

        when:
        BuildResult result = gradle (taskName, '-d')

        then:
        !result.getOutput().contains('"data"')
        result.getOutput().contains("""[{"id":"projectId","name":"Test new Project"},{"id":"projectId2","name":"Test Spark config"}]""")
    }
}
