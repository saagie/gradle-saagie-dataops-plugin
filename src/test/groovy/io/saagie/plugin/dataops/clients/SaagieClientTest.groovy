package io.saagie.plugin.dataops.clients

import io.saagie.plugin.dataops.DataOpsExtension
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.api.InvalidUserDataException
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.*

class SaagieClientTest extends Specification {
    @Shared MockWebServer mockWebServer = new MockWebServer()
    @Shared DataOpsExtension configuration
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File tempFile

    SaagieClient client

    def setupSpec() {
        mockWebServer.start(9000)
    }

    def cleanupSpec() {
        mockWebServer.shutdown()
    }

    def setup() {
        configuration = new DataOpsExtension()
        tempFile = testProjectDir.newFile('hello-world.py')
        configuration.server {
            url = 'http://localhost:9000'
            login = 'login'
            password = 'password'
            environment = 4
        }
        configuration.project {
            id = 'projectId'
        }
        configuration.job {
            name = "My custom job 2"
            category = "Extraction"
            technology = "technologyId"
        }
        configuration.jobVersion {
            runtimeVersion = "3.6"
            commandLine = "python {file} arg1 arg2"
            releaseNote = "First job version"
            packageInfo {
                name = tempFile.absolutePath
            }
        }

        client = new SaagieClient(configuration)
    }

    def cleanup() {
        mockWebServer.dispatcher.peek()
    }

    def "getProjects should return a list of projects"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """
            {"data":{"projects":[{"id":"8321e13c-892a-4481-8552-dekzdjeijzd","name":"Test new Project"},{"id":"7f5e0374-0c45-45a3-a2f3-dkjezoijdizd","name":"Test Spark config"},{"id":"bba3511b-7b7f-44f0-9f69-djeizjdoijzj","name":"For tests"},{"id":"9feae78d-1cc0-49bd-9e63-deozjiodjeiz","name":"Test simon"}]}}
        """
        mockWebServer.enqueue(mockedResponse)

        when:
        def projects = client.getProjects()

        then:
        projects instanceof String
        projects.startsWith('[{"id"')
        projects.contains('id')
        projects.contains('name')
    }

    def "getProjectJobs should return a list of project job"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """
            {"data":{"jobs":[{"name":"test2","description":"","countJobInstance":1,"versions":[{"number":1}],"category":"Processing","technology":{"id":"djeuzhduze-c18b-4ecd-b61f-dezdezdddez","label":"Python","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-03-15T14:06:49.053Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":true},{"name":"test 2.4","description":"","countJobInstance":3,"versions":[{"number":2},{"number":1}],"category":"Processing","technology":{"id":"dezddedz-26bd-4f7d-a3a5-d5dcba3935c8","label":"Spark","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-03-11T09:32:46.424Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":true}]}}
        """
        mockWebServer.enqueue(mockedResponse)

        when:
        def jobs = client.getProjectJobs()

        then:
        jobs instanceof String
        jobs.startsWith('[{"name"')
        jobs.contains('countJobInstance')
        jobs.contains('category')
    }

    def "getProjectTechnologies should return a list of project technologies"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """
            {"data":{"technologies":[{"id":"c3cadcad-fjrehf-4f7d-a3a5-frefer","label":"Spark","isAvailable":true,"icon":"spark","features":[]},{"id":"freojfier-c18b-4ecd-b61f-fjerijfiej","label":"Python","isAvailable":true,"icon":"python","features":[]},{"id":"fkiorjeiofer-c18b-4ecd-b61f-jkfijorjferferf","label":"Python","isAvailable":true,"icon":"python","features":[]},{"id":"frefreferfe-26bd-4f7d-a3a5-frejferiuh","label":"Spark","isAvailable":true,"icon":"spark","features":[]}]}}
        """
        mockWebServer.enqueue(mockedResponse)

        when:
        def technologies = client.getProjectTechnologies()

        then:
        technologies instanceof String
        technologies.startsWith('[{"id":')
        technologies.contains('features')
        technologies.contains('isAvailable')
    }

    def "createProjectJob should create a job and return the created job"() {
        given:
        def mockedJobCreationResponse = new MockResponse()
        mockedJobCreationResponse.responseCode = 200
        mockedJobCreationResponse.body = '''{"data":{"createJob":{"id":"kdiojezidz-ce2a-486e-b524-d40ff353eea7"}}}'''
        mockWebServer.enqueue(mockedJobCreationResponse)

        def mockedFileUploadResponse = new MockResponse()
        mockedFileUploadResponse.responseCode = 200
        mockedFileUploadResponse.body = '''true'''
        mockWebServer.enqueue(mockedFileUploadResponse)

        when:
        def createdJobConfig = client.createProjectJob()

        then:
        createdJobConfig instanceof String
        createdJobConfig.startsWith('{"id"')
    }

    def "createProjectJob should fail if the package file is missing"() {
        given:
        def mockedJobCreationResponse = new MockResponse()
        mockedJobCreationResponse.responseCode = 200
        mockedJobCreationResponse.body = '''{"data":{"createJob":{"id":"kdiojezidz-ce2a-486e-b524-d40ff353eea7"}}}'''
        mockWebServer.enqueue(mockedJobCreationResponse)

        def mockedFileUploadResponse = new MockResponse()
        mockedFileUploadResponse.responseCode = 500
        mockedFileUploadResponse.body = '''true'''
        mockWebServer.enqueue(mockedFileUploadResponse)

        when:
        def createdJobConfig = client.createProjectJob()

        then:
        thrown(InvalidUserDataException)
        createdJobConfig == null
    }
}
