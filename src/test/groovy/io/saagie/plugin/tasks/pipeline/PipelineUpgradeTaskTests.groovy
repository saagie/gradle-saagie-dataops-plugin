package io.saagie.plugin.tasks.pipeline

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_UPGRADE_PIPELINE_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title("projectsUpdatePipeline task tests")
class PipelineUpgradeTaskTests extends DataOpsGradleTaskSpecification {
    @Shared String taskName = PROJECTS_UPGRADE_PIPELINE_TASK

    def "projectsUpdatePipeline should update pipeline infos"() {
        given:
        def mockedPipelineUpdateResponse = new MockResponse()
        mockedPipelineUpdateResponse.responseCode = 200
        mockedPipelineUpdateResponse.body = '''{"data":{"editPipeline":{"id":"pipeline-id"}}}'''
        mockWebServer.enqueue(mockedPipelineUpdateResponse)

        def mockedPipelineVersionUpdateResponse = new MockResponse()
        mockedPipelineVersionUpdateResponse.responseCode = 200
        mockedPipelineVersionUpdateResponse.body = '''{"data":{"addPipelineVersion":{"number":"pipeline-version-number"}}}'''
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
                    description = 'updated description'
                    alerting {
                        emails = ['email@email.com']
                        statusList = ['FAILED']
                    }
                }

                pipelineVersion {
                    releaseNote = 'Updated release note'
                    jobs = ['job-id-1', 'job-id-2']
                }
            }
        '''

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{"status":"success"}')
    }

    def "projectsUpgradePipeline should fail if no pipeline id is provided"() {
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
        exception.getBuildResult().task(':projectsUpgradePipeline').outcome == FAILED
    }
}
