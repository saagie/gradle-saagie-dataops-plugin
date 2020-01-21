package io.saagie.plugin.tasks.job

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('projectsCreateJob task tests')
class JobCreateTaskTests extends DataOpsGradleTaskSpecification {
    @Shared String taskName = 'projectsCreateJob'

    def "projectsCreateJob should fail if the file to upload doesn't exists"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
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
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains("Check that there is a file to upload in 'bad/path/to-file.sh'. Be sure that 'bad/path/to-file.sh' is a correct file path.")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

    def "projectsCreateJob should fail if the job name is already taken"() {
        given:
        def mockedJobCreationResponse = new MockResponse()
        mockedJobCreationResponse.responseCode = 200
        mockedJobCreationResponse.body = '''{"errors":[{"message":"Job not valid","extensions":{"name":"already used","classification":"ValidationError"}}],"data":null}'''
        mockWebServer.enqueue(mockedJobCreationResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 2
                }

                project {
                    id = 'projectId'
                }

                job {
                    name = "Already taken name"
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
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains('Something went wrong when creating project job: {"errors":[{"message":"Job not valid","extensions":{"name":"already used","classification":"ValidationError"}}],"data":null}')
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

    def "projectsCreateJob should create job and upload a file"() {
        given:
        enqueueRequest('{"data":{"createJob":{"id":"jobId","name":"Created Job"}}}')

        buildFile << """
            saagie {
                server {
                    url = '${mockServerUrl}'
                    login = 'user'
                    password = 'password'
                    environment = 2
                }

                project {
                    id = 'projectId'
                }

                job {
                    name = "jobname"
                    category = "Extraction"
                    technology = "technology-id"
                }

                jobVersion {
                    runtimeVersion = "3.6"
                    commandLine = "python {file} arg1 arg2"
                    packageInfo {
                        name = "${jobFile.absolutePath}"
                    }
                }
            }
        """

        when:
        def result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{"id":"jobId","name":"Created Job"}')
    }

    def "projectsCreateJob should use the deprecated api if useLegacy is provided"() {
        def mockedJobCreationResponse = new MockResponse()
        mockedJobCreationResponse.responseCode = 200
        mockedJobCreationResponse.body = '''{"data":{"createJob":{"id":"jobId","name":"Created Job"}}}'''
        mockWebServer.enqueue(mockedJobCreationResponse)

        def mockedAddJobVersionResponse = new MockResponse()
        mockedAddJobVersionResponse.responseCode = 200
        mockedAddJobVersionResponse.body = '''{"data":{"addJobVersion":{"number":3}}}'''
        mockWebServer.enqueue(mockedAddJobVersionResponse)

        def mockedFileUploadResponse = new MockResponse()
        mockedFileUploadResponse.responseCode = 200
        mockedFileUploadResponse.body = 'true'
        mockWebServer.enqueue(mockedFileUploadResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 2
                    useLegacy = true
                }

                project {
                    id = 'projectId'
                }

                job {
                    name = "jobname"
                    category = "Extraction"
                    technology = "technology-id"
                }

                jobVersion {
                    runtimeVersion = "3.6"
                    commandLine = "python {file} arg1 arg2"
                    packageInfo {
                        name = "${jobFile.absolutePath}"
                    }
                }
            }
        """

        when:
        def result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{"id":"jobId","name":"Created Job"}')
    }

    def "deprecated projectsCreateJob should fail if job config is missing required params"() {
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 2
                    useLegacy = true
                }

                project {
                    id = 'projectId'
                }

                job {
                    description = "description"
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure error = thrown()
        result == null
        error.message.contains("Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/${taskName}")
        error.getBuildResult().task(":${taskName}").outcome == FAILED
    }

    def "deprecated projectsCreateJob should fail if no jobVersion is provided"() {
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 2
                    useLegacy = true
                }

                project {
                    id = 'projectId'
                }

                job {
                    name = "jobname"
                    category = "Extraction"
                    technology = "technology-id"
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure error = thrown()
        result == null
        error.message.contains("Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/${taskName}")
        error.getBuildResult().task(":${taskName}").outcome == FAILED
    }

    def "deprecated projectsCreateJob should not add version if the file is invalid"() {
        def mockedJobCreationResponse = new MockResponse()
        mockedJobCreationResponse.responseCode = 200
        mockedJobCreationResponse.body = '''{"data":{"createJob":{"id":"jobId","name":"Created Job"}}}'''
        mockWebServer.enqueue(mockedJobCreationResponse)

        def mockedAddJobVersionResponse = new MockResponse()
        mockedAddJobVersionResponse.responseCode = 200
        mockedAddJobVersionResponse.body = '''{"data":{"addJobVersion":{"number":3}}}'''
        mockWebServer.enqueue(mockedAddJobVersionResponse)

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user'
                    password = 'password'
                    environment = 2
                    useLegacy = true
                }

                project {
                    id = 'projectId'
                }

                job {
                    name = "jobname"
                    category = "Extraction"
                    technology = "technology-id"
                }

                jobVersion {
                    runtimeVersion = "3.6"
                    commandLine = "python {file} arg1 arg2"
                    packageInfo {
                        name = "invalid-file/path"
                    }
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure error = thrown()
        result == null
        error.message.contains("Check that there is a file to upload in 'invalid-file/path'. Be sure that 'invalid-file/path' is a correct file path.")
        error.getBuildResult().task(":${taskName}").outcome == FAILED
    }
}
