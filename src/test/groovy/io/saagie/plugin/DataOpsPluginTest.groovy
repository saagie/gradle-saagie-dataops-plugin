package io.saagie.plugin

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.*

@Title("Plugin integration test with gradle")
class DataOpsPluginTest extends Specification {
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

    // ===========================
    // TESTS =====================
    def "projectsList task should return a list of project"() {
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
        def result = gradle 'projectsList'

        then:
        !result.output.contains('"data"')
        result.output.contains('[{"id":"8321e13c')
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
        gradle 'projectsList'

        then:
        thrown(Exception)
    }

    def "projectsListJobs task should list jobs on a project"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """
            {"data":{"jobs":[{"name":"test2","description":"","countJobInstance":1,"versions":[{"number":1}],"category":"Processing","technology":{"id":"frefref-c18b-4ecd-b61f-frefefreff","label":"Python","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-03-15T14:06:49.053Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":true},{"name":"test 2","description":"","countJobInstance":4,"versions":[{"number":2},{"number":0}],"category":"Processing","technology":{"id":"dezded-26bd-4f7d-a3a5-dezdedzdz","label":"Spark","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-03-11T09:32:46.424Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":true}]}}
        """
        buildFile << '''
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
        '''
        mockWebServer.enqueue(mockedResponse)

        when:
        def result = gradle 'projectsListJobs'

        then:
        !result.output.contains('"data"')
        result.output.contains('"name"')
        result.output.contains('"countJobInstance"')
    }

    def "projectsListJobs task should fail if bad project config is provided"() {
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
                
                }
            }
        """

        when:
        gradle 'projectsListJobs'

        then:
        thrown(Exception)
    }

    def "projectsListJobs task should fail if a wrong project id is provided"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """{"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}"""

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
        gradle 'projectsListJobs'

        then:
        thrown(Exception)
    }

    def "projectsListTechnologies task should list technologies of a project"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """
            {"data":{"technologies":[{"id":"c3cadcad-fjrehf-4f7d-a3a5-frefer","label":"Spark","isAvailable":true,"icon":"spark","features":[]},{"id":"freojfier-c18b-4ecd-b61f-fjerijfiej","label":"Python","isAvailable":true,"icon":"python","features":[]},{"id":"fkiorjeiofer-c18b-4ecd-b61f-jkfijorjferferf","label":"Python","isAvailable":true,"icon":"python","features":[]},{"id":"frefreferfe-26bd-4f7d-a3a5-frejferiuh","label":"Spark","isAvailable":true,"icon":"spark","features":[]}]}}
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
        def result = gradle 'projectsListTechnologies'

        then:
        !result.output.contains('"data"')
        result.output.contains('"label"')
        result.output.contains('"features"')
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

    def "projectsRunJob should run a job and return the job instance id and status"() {
        given:
        def mockedRunJobResponse = new MockResponse()
        mockedRunJobResponse.responseCode = 200
        mockedRunJobResponse.body = '''{"data":{"runJob":{"id":"job-instance-id","status":"REQUESTED"}}}'''
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
        BuildResult result = gradle 'projectsRunJob'

        then:
        !result.output.contains('"data"')
        result.output.contains('"id"')
        result.output.contains('"status"')
    }

    def "projectsRunJob should fail if job or job id is null"() {
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
        BuildResult result = gradle 'projectsRunJob'

        then:
        thrown(Exception)
        result == null
    }

    def "projectsRunJob should fail if job id doesn't exists"() {
        given:
        def mockedRunJobResponse = new MockResponse()
        mockedRunJobResponse.responseCode = 200
        mockedRunJobResponse.body = '''{"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}'''
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
        BuildResult result = gradle 'projectsRunJob'
        println result.output

        then:
        thrown(Exception)
        result == null
    }

    def "projectUpdateJob should update the specified job"() {
        given:
        def mockedJobCreationResponse = new MockResponse()
        mockedJobCreationResponse.responseCode = 200
        mockedJobCreationResponse.body = '''{"data":{"createJob":{"id":"kdiojezidz-ce2a-486e-b524-d40ff353eea7"}}}'''
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
            }
        """

        when:
        BuildResult result = gradle 'projectUpdateJob'

        then:
        result.output.contains('"id"')
    }
}
