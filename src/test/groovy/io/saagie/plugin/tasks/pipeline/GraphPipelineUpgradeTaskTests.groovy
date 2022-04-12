package io.saagie.plugin.tasks.pipeline

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_UPGRADE_GRAPH_PIPELINE_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title("projectsUpdatePipeline task tests")
class GraphPipelineUpgradeTaskTests extends DataOpsGradleTaskSpecification {
    @Shared
    String taskName = PROJECTS_UPGRADE_GRAPH_PIPELINE_TASK

    def "projectsUpdateGraphPipeline should update pipeline infos"() {
        given:
        def mockedPipelineUpdateResponse = new MockResponse()
        mockedPipelineUpdateResponse.responseCode = 200
        mockedPipelineUpdateResponse.body = '''{"data":{"editPipeline":{"id":"pipeline-id"}}}'''
        mockWebServer.enqueue(mockedPipelineUpdateResponse)

        def mockedPipelineVersionUpdateResponse = new MockResponse()
        mockedPipelineVersionUpdateResponse.responseCode = 200
        mockedPipelineVersionUpdateResponse.body = '''{"data":{"addGraphPipelineVersion":{"number":"pipeline-version-number"}}}'''
        mockWebServer.enqueue(mockedPipelineVersionUpdateResponse)

        buildFile << '''
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.login'
                    password = 'password'
                    environment = 1
                }

                pipeline {
                    id = 'pipelineId'
                    name = 'Pipeline updated'
                    executionVariables = 'toto=6'
                    hasExecutionVariablesEnabled = false
                    description = 'updated description'
                    alerting {
                        emails = ['email@email.com']
                        statusList = ['FAILED']
                    }
                }

                pipelineVersion {
                    releaseNote = "new release note version"
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
        '''

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{"status":"success"}')
    }

    def "projectsUpgradeGraphPipeline should fail if no pipeline id is provided"() {
        given:

        buildFile << '''
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.login'
                    password = 'password'
                    environment = 1
                }

                pipeline {
                    name = 'Pipeline updated'
                    description = 'updated description'
                    alerting {
                        emails = ['email@email.com']
                        statusList = ['FAILED']
                    }
                }
            }
        '''

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure exception = thrown()
        result == null
        exception.message.contains("Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/${taskName}")
        exception.getBuildResult().task(':projectsUpgradeGraphPipeline').outcome == FAILED
    }
}
