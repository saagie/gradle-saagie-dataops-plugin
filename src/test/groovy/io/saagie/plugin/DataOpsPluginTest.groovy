package io.saagie.plugin

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.*

class DataOpsPluginTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    @Shared MockWebServer mockWebServer = new MockWebServer()

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


    // ===========================
    // TESTS =====================
    def "projectList task should return a list of project"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """
            {"data":{"projects":[{"id":"8321e13c-892a-4481-8552-dekzdjeijzd","name":"Test new Project"},{"id":"7f5e0374-0c45-45a3-a2f3-dkjezoijdizd","name":"Test Spark config"},{"id":"bba3511b-7b7f-44f0-9f69-djeizjdoijzj","name":"For tests"},{"id":"9feae78d-1cc0-49bd-9e63-deozjiodjeiz","name":"Test simon"}]}}
        """
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
        def result = gradle 'projectList'

        then:
        !result.output.contains('"data"')
        result.output.contains('[{"id":"8321e13c')
    }

    def "projectList task with bad config should fail"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = 'https://saagie-beta.prod.saagie.io/'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }
            }
        """

        when:
        def result = gradle('projectList')

        then:
        thrown(Exception)
    }

    def "projectListJobs task should list jobs on a project"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """
            {"data":{"jobs":[{"name":"test2","description":"","countJobInstance":1,"versions":[{"number":1}],"category":"Processing","technology":{"id":"frefref-c18b-4ecd-b61f-frefefreff","label":"Python","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-03-15T14:06:49.053Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":true},{"name":"test 2","description":"","countJobInstance":4,"versions":[{"number":2},{"number":0}],"category":"Processing","technology":{"id":"dezded-26bd-4f7d-a3a5-dezdedzdz","label":"Spark","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-03-11T09:32:46.424Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":true}]}}
        """
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }
                
                project {
                    id = 'dezdezjiodjei-892a-2342-8552-5be4b6de5df4'
                }
            }
        """
        mockWebServer.enqueue(mockedResponse)

        when:
        def result = gradle 'projectListJobs'

        then:
        !result.output.contains('"data"')
        result.output.contains('"name"')
        result.output.contains('"countJobInstance"')
    }

    def "projectListJobs task should fail if no project config is provided"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """
            {"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}
        """
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }
                
                project {
                    
                }
            }
        """
        mockWebServer.enqueue(mockedResponse)

        when:
        def result = gradle 'projectListJobs'

        then:
        thrown(Exception)
    }

    def "projectListJobs task should fail if a wrong project id is provided"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """
            {"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}
        """
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }
                
                project {
                    id = 'wrong id'
                }
            }
        """
        mockWebServer.enqueue(mockedResponse)

        when:
        gradle 'projectListJobs'

        then:
        thrown(Exception)
    }

}
