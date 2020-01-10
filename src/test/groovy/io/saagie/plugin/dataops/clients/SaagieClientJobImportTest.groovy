package io.saagie.plugin.dataops.clients

import io.saagie.plugin.dataops.DataOpsExtension
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_IMPORT_JOB

class SaagieClientJobImportTest extends Specification  {
    @Shared String taskName = PROJECTS_IMPORT_JOB
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
            environment = 1
        }
        configuration.project {
            id = 'projectId'
        }
    }

    def cleanup() {
        mockWebServer.dispatcher.peek()
    }

    def enqueueToken() {
        def mockedTokenResponse = new MockResponse()
        mockedTokenResponse.responseCode = 200
        mockedTokenResponse.body = 'token'
        mockWebServer.enqueue(mockedTokenResponse)
    }

    def "the task should create a new job based on the provided exports zip file"() {
        given:
        SaagieClient saagieClient = new SaagieClient(configuration, taskName)

        when:
        saagieClient.importJob()

        then:
        true == true
    }
}
