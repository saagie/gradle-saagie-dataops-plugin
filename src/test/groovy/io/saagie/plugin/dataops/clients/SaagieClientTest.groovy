package io.saagie.plugin.dataops.clients

import io.saagie.plugin.dataops.DataOpsExtension
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import spock.lang.*

class SaagieClientTest extends Specification {
    @Shared MockWebServer mockWebServer = new MockWebServer()
    @Shared DataOpsExtension configuration
    SaagieClient client

    def setupSpec() {
        mockWebServer.start(9000)
    }

    def cleanupSpec() {
        mockWebServer.shutdown()
    }

    def setup() {
        configuration = new DataOpsExtension()
//        configuration.server {
//            url = 'http://localhost:9000'
//            login = 'login'
//            password = 'password'
//            environment = 4
//        }
//        configuration.project {
//            id = 3
//        }
//        configuration.job {
//            name = "My custom job 2"
//            category = "Extraction"
//            technology = "technologyId"
//        }
//        configuration.jobVersion {
//            runtimeVersion = "3.6"
//            commandLine = "python {file} arg1 arg2"
//            releaseNote = "First job version"
//            packageInfo {
//                name = "hello-world.py"
//            }
//        }
        configuration.server {
            url = 'https://saagie-beta.prod.saagie.io'
            login = 'renan.decamps'
            password = 'McVities$76%!1994'
            environment = 4
        }
        configuration.project {
            id = "ec8c5cea-3dfd-4496-87a0-89f69101dccd"
        }
        configuration.job {
            name = "Job created from gradle 3"
            category = "Extraction"
            technology = "13522063-c18b-4ecd-b61f-3bae1e0ad93c"
        }
        configuration.jobVersion {
            runtimeVersion = "3.6"
            commandLine = "python {file} arg1 arg2"
            releaseNote = "First job version"
            packageInfo {
                name = "/Users/orangina/Desktop/hello-world.py"
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
        projects.startsWith('[{"id":"8321e13c')
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
        jobs.startsWith('[{"name":"test2"')
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
        technologies.startsWith('[{"id":"c3cadcad')
        technologies.contains('features')
        technologies.contains('isAvailable')
    }

    def "createProjectJob should create a job and return the created job"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """
            // TODO: get a fake body response
        """
        mockWebServer.enqueue(mockedResponse)

        when:
        def createdJobConfig = client.createProjectJob()

        then:
        createdJobConfig instanceof String
        createdJobConfig.startsWith('{"id"')
    }

}
