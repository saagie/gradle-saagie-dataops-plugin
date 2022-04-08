package io.saagie.plugin.tasks.pipeline

import io.saagie.plugin.DataOpsGradleTaskSpecification
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.utils.SaagieUtils
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okio.Buffer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_CREATE_GRAPH_PIPELINE_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('projectsCreateGraphPipeline task tests')
class GraphPipelineCreateTaskTests extends DataOpsGradleTaskSpecification {
    @Shared
    String taskName = PROJECTS_CREATE_GRAPH_PIPELINE_TASK

    def "projectsCreateGraphPipelineJob should create a new pipeline"() {
        given:
        def mockedCreatePipelineResponse = new MockResponse()
        mockedCreatePipelineResponse.responseCode = 200
        mockedCreatePipelineResponse.body = '''{"data":{"createGraphPipeline":{"id":"pipeline-id"}}}'''
        mockWebServer.enqueue(mockedCreatePipelineResponse)

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

                pipeline {
                    name = 'Pipeline name'
                    executionVariables = 'toto=6'
                    hasExecutionVariablesEnabled = false
                }

                pipelineVersion {
                    releaseNote = "pipeline version 1"
                    graph {
                        jobNodes = [
                            {
                                id = "72187690-d7f4-11eb-b632-f10974faf871"
                                position {
                                    x = 650
                                    y = 90
                                }
                                nextNodes = ["12187690-d7f4-11eb-b632-f10974faf871"]
                                job {
                                    id = "bb42bbe1-946a-4e3d-b8df-b3c8a1846869"
                                }
                            },
                            {
                                id = "81f96790-d7f4-11eb-b632-f10974faf871"
                                position {
                                    x = 670
                                    y = 190
                                }
                                nextNodes = []
                                job {
                                    id = "bb42bbe1-946a-4e3d-b8df-b3c8a1846869"
                                }
                            },
                            {
                                id = "71f96790-d7f4-11eb-b632-f10974faf871"
                                nextNodes = ["81f96790-d7f4-11eb-b632-f10974faf871"]
                                job {
                                    id = "bb42bbe1-946a-4e3d-b8df-b3c8a1846869"
                                }
                            }
                        ]
                        conditionNodes = [
                            {
                                id = "12187690-d7f4-11eb-b632-f10974faf871"
                                nextNodesSuccess = ["71f96790-d7f4-11eb-b632-f10974faf871"]
                                nextNodesFailure = ["81f96790-d7f4-11eb-b632-f10974faf871"]
                            }
                        ]
                    }
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        result.output.contains('{"id":"pipeline-id"}')
    }

    def "projectsCreateGraphPipelineJob should fail if required config is missing"() {
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

    def "projectsCreateGraphPipelineJob should fail if there is already a pipeline with the same name"() {
        given:
        def mockedCreatePipelineResponse = new MockResponse()
        mockedCreatePipelineResponse.responseCode = 200
        mockedCreatePipelineResponse.body = '''{"data":null,"errors":[{"cause":null,"extensions":{"name":"already used"},"locations":null,"errorType":"ValidationError","message":"Pipeline not valid","path":null,"localizedMessage":"Pipeline not valid","suppressed":[]}]}'''
        mockWebServer.enqueue(mockedCreatePipelineResponse)

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

                pipeline {
                    name = 'Pipeline name already used'
                }

                pipelineVersion {
                    releaseNote = "pipeline version 1"
                    graph {
                        jobNodes = [
                            {
                                id = "72187690-d7f4-11eb-b632-f10974faf871"
                                position {
                                    x = 650
                                    y = 90
                                }
                                nextNodes = ["12187690-d7f4-11eb-b632-f10974faf871"]
                                job {
                                    id = "bb42bbe1-946a-4e3d-b8df-b3c8a1846869"
                                }
                            },
                            {
                                id = "81f96790-d7f4-11eb-b632-f10974faf871"
                                position {
                                    x = 670
                                    y = 190
                                }
                                nextNodes = []
                                job {
                                    id = "bb42bbe1-946a-4e3d-b8df-b3c8a1846869"
                                }
                            },
                            {
                                id = "71f96790-d7f4-11eb-b632-f10974faf871"
                                nextNodes = ["81f96790-d7f4-11eb-b632-f10974faf871"]
                                job {
                                    id = "bb42bbe1-946a-4e3d-b8df-b3c8a1846869"
                                }
                            }
                        ]
                        conditionNodes = [
                            {
                                id = "12187690-d7f4-11eb-b632-f10974faf871"
                                nextNodesSuccess = ["71f96790-d7f4-11eb-b632-f10974faf871"]
                                nextNodesFailure = ["81f96790-d7f4-11eb-b632-f10974faf871"]
                            }
                        ]
                    }
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains('{"data":null,"errors":[{"cause":null,"extensions":{"name":"already used"},"locations":null,"errorType":"ValidationError","message":"Pipeline not valid","path":null,"localizedMessage":"Pipeline not valid","suppressed":[]}]}')
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

    def "projectsCreateGraphPipelineJob should not add empty alerting if no alerting config is provided"() {
        given:
        DataOpsExtension config = new DataOpsExtension()
        config.with {
            server {
                url = 'http://localhost:9000'
                login = 'login'
                password = 'password'
                environment = 1
            }

            project {
                id = 'projectId'
            }

            pipeline {
                name = 'Pipeline name'
            }

            pipelineVersion {
                releaseNote = "pipeline version 1"
                graph {
                    jobNodes = [
                        {
                            id = "72187690-d7f4-11eb-b632-f10974faf871"
                            position {
                                x = 650
                                y = 90
                            }
                            nextNodes = ["12187690-d7f4-11eb-b632-f10974faf871"]
                            job {
                                id = "bb42bbe1-946a-4e3d-b8df-b3c8a1846869"
                            }
                        },
                        {
                            id = "81f96790-d7f4-11eb-b632-f10974faf871"
                            position {
                                x = 670
                                y = 190
                            }
                            nextNodes = []
                            job {
                                id = "bb42bbe1-946a-4e3d-b8df-b3c8a1846869"
                            }
                        },
                        {
                            id = "71f96790-d7f4-11eb-b632-f10974faf871"
                            nextNodes = ["81f96790-d7f4-11eb-b632-f10974faf871"]
                            job {
                                id = "bb42bbe1-946a-4e3d-b8df-b3c8a1846869"
                            }
                        }
                    ]
                    conditionNodes = [
                        {
                            id = "12187690-d7f4-11eb-b632-f10974faf871"
                            nextNodesSuccess = ["71f96790-d7f4-11eb-b632-f10974faf871"]
                            nextNodesFailure = ["81f96790-d7f4-11eb-b632-f10974faf871"]
                        }
                    ]
                }
            }
        }
        SaagieUtils saagieUtils = new SaagieUtils(config)

        when:
        Request request = saagieUtils.getCreatePipelineRequest(config.pipeline, config.pipelineVersion)
        final Buffer buffer = new Buffer()
        request.body().writeTo(buffer)
        String body = buffer.readUtf8()

        then:
        notThrown(Exception)
        !body.contains('"alerting":{"emails":[],"statusList":[],"logins":[]}')
    }

    def "projectsCreateGraphPipelineJob should contain alerting if alerting config is provided"() {
        given:
        DataOpsExtension config = new DataOpsExtension()
        config.with {
            server {
                url = 'http://localhost:9000'
                login = 'login'
                password = 'password'
                environment = 1
            }

            project {
                id = 'projectId'
            }

            pipeline {
                name = 'Pipeline name'
                alerting {
                    emails = ['email@email.com']
                    logins = ['login']
                    statusList = ['FAILED']
                }
            }

            pipelineVersion {
                releaseNote = "pipeline version 1"
                graph {
                    jobNodes = [
                        {
                            id = "72187690-d7f4-11eb-b632-f10974faf871"
                            position {
                                x = 650
                                y = 90
                            }
                            nextNodes = ["12187690-d7f4-11eb-b632-f10974faf871"]
                            job {
                                id = "bb42bbe1-946a-4e3d-b8df-b3c8a1846869"
                            }
                        },
                        {
                            id = "81f96790-d7f4-11eb-b632-f10974faf871"
                            position {
                                x = 670
                                y = 190
                            }
                            nextNodes = []
                            job {
                                id = "bb42bbe1-946a-4e3d-b8df-b3c8a1846869"
                            }
                        },
                        {
                            id = "71f96790-d7f4-11eb-b632-f10974faf871"
                            nextNodes = ["81f96790-d7f4-11eb-b632-f10974faf871"]
                            job {
                                id = "bb42bbe1-946a-4e3d-b8df-b3c8a1846869"
                            }
                        }
                    ]
                    conditionNodes = [
                        {
                            id = "12187690-d7f4-11eb-b632-f10974faf871"
                            nextNodesSuccess = ["71f96790-d7f4-11eb-b632-f10974faf871"]
                            nextNodesFailure = ["81f96790-d7f4-11eb-b632-f10974faf871"]
                        }
                    ]
                }
            }
        }
        SaagieUtils saagieUtils = new SaagieUtils(config)

        when:
        Request request = saagieUtils.getCreatePipelineRequest(config.pipeline, config.pipelineVersion)
        final Buffer buffer = new Buffer()
        request.body().writeTo(buffer)
        String body = buffer.readUtf8()

        then:
        notThrown(Exception)
        body.contains('"alerting":{"logins":["login"],"statusList":["FAILED"]}')
    }
}
