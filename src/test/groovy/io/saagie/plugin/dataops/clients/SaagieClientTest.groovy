package io.saagie.plugin.dataops.clients

import io.saagie.plugin.dataops.DataOpsExtension
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.api.GradleException
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
            environment = 1
            jwt = true
            realm = 'userrealm'
        }
        configuration.project {
            id = 'projectId'
        }
        configuration.job {
            id = 'jobId'
            name = 'My custom job 2'
            category = 'Extraction'
            technology = 'technologyId'
        }
        configuration.jobVersion {
            runtimeVersion = '3.6'
            commandLine = 'python {file} arg1 arg2'
            releaseNote = 'First job version'
            packageInfo {
                name = tempFile.absolutePath
            }
        }
        configuration.jobinstance {
            id = 'jobInstanceId'
        }
        configuration.pipeline {
            name = 'Pipeline name'
        }
        configuration.pipelineVersion {
            releaseNote = 'Release note'
            jobs = ['jobId-1', 'jobId-2']
        }
        configuration.pipelineinstance {
            id = 'pipelineInstanceId'
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

    def "class instance must throw an error if required parameters are missing"() {
        given:
        DataOpsExtension badConfig = new DataOpsExtension()
        badConfig.server {
            url = null
            login = null
            password = null
            environment = null
        }

        when:
        new SaagieClient(badConfig, 'projectsList')

        then:
        InvalidUserDataException e = thrown()
        e.message.contains('Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/projectsList')
    }

    def "class instance with a jwt option have to request a jwtToken"() {
        given:
        DataOpsExtension config = new DataOpsExtension()
        config.server {
            url = 'http://localhost:9000'
            login = 'login'
            password = 'password'
            environment = 1
            jwt = true
            realm = 'userrealm' // only for test
        }

        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = 'a very long token'
        mockWebServer.enqueue(mockedResponse)

        when:
        def client = new SaagieClient(config, 'projectsList')

        then:
        client.configuration.server.realm == 'USERREALM'
        client.configuration.server.token == 'a very long token'
    }

    def "class instance with a jwt option and bad credentials must fail"() {
        given:
        DataOpsExtension badConfig = new DataOpsExtension()
        badConfig.server {
            url = 'http://localhost:9000'
            login = 'bad login'
            password = 'bad password'
            environment = 1
            jwt = true
            realm = 'userrealm'
        }

        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 401
        mockedResponse.body = 'Bad credentials'
        mockWebServer.enqueue(mockedResponse)

        when:
        new SaagieClient(badConfig, 'projectsList')

        then:
        GradleException e = thrown()
        e.message.contains('Error 401 when requesting on http://localhost:9000:\nBad credentials')
    }

    def "class instance with a config with a trailing slash in the url must succeed"() {
        given:
        DataOpsExtension badConfig = new DataOpsExtension()
        badConfig.server {
            url = 'http://localhost:9000/'
            login = 'login'
            password = 'password'
            environment = 1
            jwt = true
            realm = 'userrealm'
        }

        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = 'ok'
        mockWebServer.enqueue(mockedResponse)

        when:
        SaagieClient client = new SaagieClient(badConfig, 'projectsList')

        then:
        !client.configuration.server.url.endsWith('/')
    }

    def "getProjects should return a list of projects"() {
        given:
        enqueueToken()

        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """
            {"data":{"projects":[{"id":"8321e13c-892a-4481-8552-dekzdjeijzd","name":"Test new Project"},{"id":"7f5e0374-0c45-45a3-a2f3-dkjezoijdizd","name":"Test Spark config"},{"id":"bba3511b-7b7f-44f0-9f69-djeizjdoijzj","name":"For tests"},{"id":"9feae78d-1cc0-49bd-9e63-deozjiodjeiz","name":"Test simon"}]}}
        """
        mockWebServer.enqueue(mockedResponse)
        client = new SaagieClient(configuration, 'projectsList')

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
        enqueueToken()

        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """
            {"data":{"jobs":[{"name":"test2","description":"","countJobInstance":1,"versions":[{"number":1}],"category":"Processing","technology":{"id":"djeuzhduze-c18b-4ecd-b61f-dezdezdddez","label":"Python","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-03-15T14:06:49.053Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":true},{"name":"test 2.4","description":"","countJobInstance":3,"versions":[{"number":2},{"number":1}],"category":"Processing","technology":{"id":"dezddedz-26bd-4f7d-a3a5-d5dcba3935c8","label":"Spark","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-03-11T09:32:46.424Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":true}]}}
        """
        mockWebServer.enqueue(mockedResponse)
        client = new SaagieClient(configuration, 'projectsList')

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
        enqueueToken()

        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = """
            {"data":{"technologies":[{"id":"c3cadcad-fjrehf-4f7d-a3a5-frefer","label":"Spark","isAvailable":true,"icon":"spark","features":[]},{"id":"freojfier-c18b-4ecd-b61f-fjerijfiej","label":"Python","isAvailable":true,"icon":"python","features":[]},{"id":"fkiorjeiofer-c18b-4ecd-b61f-jkfijorjferferf","label":"Python","isAvailable":true,"icon":"python","features":[]},{"id":"frefreferfe-26bd-4f7d-a3a5-frejferiuh","label":"Spark","isAvailable":true,"icon":"spark","features":[]}]}}
        """
        mockWebServer.enqueue(mockedResponse)
        client = new SaagieClient(configuration, 'projectsList')

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
        enqueueToken()

        def mockedJobCreationResponse = new MockResponse()
        mockedJobCreationResponse.responseCode = 200
        mockedJobCreationResponse.body = '''{"data":{"createJob":{"id":"kdiojezidz-ce2a-486e-b524-d40ff353eea7"}}}'''
        mockWebServer.enqueue(mockedJobCreationResponse)

        def mockedFileUploadResponse = new MockResponse()
        mockedFileUploadResponse.responseCode = 200
        mockedFileUploadResponse.body = '''true'''
        mockWebServer.enqueue(mockedFileUploadResponse)
        client = new SaagieClient(configuration, 'projectsList')

        when:
        def createdJobConfig = client.createProjectJob()

        then:
        createdJobConfig instanceof String
        createdJobConfig.startsWith('{"id"')
    }

    def "createProjectJob should fail if the package file is missing"() {
        given:
        enqueueToken()

        def mockedJobCreationResponse = new MockResponse()
        mockedJobCreationResponse.responseCode = 200
        mockedJobCreationResponse.body = '''{"data":{"createJob":{"id":"kdiojezidz-ce2a-486e-b524-d40ff353eea7"}}}'''
        mockWebServer.enqueue(mockedJobCreationResponse)

        def mockedFileUploadResponse = new MockResponse()
        mockedFileUploadResponse.responseCode = 500
        mockedFileUploadResponse.body = '''true'''
        mockWebServer.enqueue(mockedFileUploadResponse)
        client = new SaagieClient(configuration, 'projectsList')

        when:
        def createdJobConfig = client.createProjectJob()

        then:
        GradleException exception = thrown()
        createdJobConfig == null
        exception.message.contains('Error 500 when requesting on http://localhost:9000:\ntrue')
    }

    def "runProjectJob should fail there is no job id config"() {
        given:
        enqueueToken()

        client = new SaagieClient(configuration, 'projectsList')
        client.configuration.job.id = null

        when:
        String runJobConfig = client.runProjectJob()

        then:
        InvalidUserDataException exception = thrown()
        runJobConfig == null
        exception.message.contains('Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/projectsList')
    }

    def "runProjectJob should run the provided job and return instance id of the job"() {
        given:
        enqueueToken()

        def mockedRunJobResponse = new MockResponse()
        mockedRunJobResponse.responseCode = 200
        mockedRunJobResponse.body = '''{"data":{"runJob":{"id":"job-instance-id","status":"REQUESTED"}}}'''
        mockWebServer.enqueue(mockedRunJobResponse)
        client = new SaagieClient(configuration, 'projectsList')

        when:
        String runJobConfig = client.runProjectJob()

        then:
        runJobConfig == '{"id":"job-instance-id","status":"REQUESTED"}'
    }

    def "getJobInstanceStatus should return the status of the provided job instance"() {
        given:
        enqueueToken()

        def mockedRunJobResponse = new MockResponse()
        mockedRunJobResponse.responseCode = 200
        mockedRunJobResponse.body = '''{"data":{"jobInstance":{"status":"SUCCEEDED"}}}'''
        mockWebServer.enqueue(mockedRunJobResponse)
        client = new SaagieClient(configuration, 'projectsList')

        when:
        String getJobInstanceStatusResult = client.getJobInstanceStatus()

        then:
        getJobInstanceStatusResult == '{"status":"SUCCEEDED"}'
    }

    def "getJobInstanceStatus should fail if no jobInstance is provided"() {
        given:
        enqueueToken()

        client = new SaagieClient(configuration, 'projectsList')
        client.configuration.jobinstance.id = null

        when:
        String getJobInstanceStatusResult = client.getJobInstanceStatus()

        then:
        InvalidUserDataException exception = thrown()
        getJobInstanceStatusResult == null
        exception.message.contains('Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/projectsList')
    }

    def "getJobInstanceStatus should fail if bad response is returned"() {
        given:
        enqueueToken()

        def mockedRunJobResponse = new MockResponse()
        mockedRunJobResponse.responseCode = 200
        mockedRunJobResponse.body = '''{"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}'''
        mockWebServer.enqueue(mockedRunJobResponse)
        client = new SaagieClient(configuration, 'projectsList')

        when:
        String getJobInstanceStatusResult = client.getJobInstanceStatus()

        then:
        GradleException exception = thrown()
        getJobInstanceStatusResult == null
        exception.message.contains('Something went wrong when requesting job instance status: {"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}')
    }

    def "createPipelineJob should create a new pipeline"() {
        given:
        enqueueToken()

        def mockedCreatePipelineResponse = new MockResponse()
        mockedCreatePipelineResponse.responseCode = 200
        mockedCreatePipelineResponse.body = '''{"data":{"createPipeline":{"id":"pipeline-instance-id"}}}'''
        mockWebServer.enqueue(mockedCreatePipelineResponse)
        client = new SaagieClient(configuration, 'projectsList')

        when:
        String createPipelineJobConfig = client.createProjectPipelineJob()

        then:
        createPipelineJobConfig == '{"id":"pipeline-instance-id"}'
    }

    def "getPipelineInstanceStatus should fail if no pipelineInstance is provided"() {
        given:
        enqueueToken()

        client = new SaagieClient(configuration, 'projectsList')
        client.configuration.pipelineinstance.id = null

        when:
        String getPipelineInstanceStatusResult = client.getPipelineInstanceStatus()

        then:
        InvalidUserDataException exception = thrown()
        getPipelineInstanceStatusResult == null
        exception.message.contains('Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/projectsList')
    }

    def "getPipelineInstanceStatus should fail if bad response is returned"() {
        given:
        enqueueToken()

        def mockedGetPipelineStatusResponse = new MockResponse()
        mockedGetPipelineStatusResponse.responseCode = 200
        mockedGetPipelineStatusResponse.body = '''{"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}'''
        mockWebServer.enqueue(mockedGetPipelineStatusResponse)
        client = new SaagieClient(configuration, 'projectsList')

        when:
        String getPipelineInstanceStatusResult = client.getPipelineInstanceStatus()

        then:
        GradleException exception = thrown()
        getPipelineInstanceStatusResult == null
        exception.message.contains('Something went wrong when requesting pipeline instance status: {"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}')
    }

    def "realm must be uppercased"() {
        given:
        enqueueToken()
        configuration.server.realm = 'userrealm'
        configuration.server.jwt = true

        when:
        client = new SaagieClient(configuration, 'projectsList')

        then:
        notThrown(Exception)
        client.configuration.server.realm == 'USERREALM'
    }
}
