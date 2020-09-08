package io.saagie.plugin.tasks.pipeline

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_DELETE_PIPELINE_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('projectsDeletePipeline task tests')
class PipelineDeleteTaskTests extends DataOpsGradleTaskSpecification {
	@Shared
	String taskName = PROJECTS_DELETE_PIPELINE_TASK
	
	def "projectsDeletePipeline should delete a pipeline the deletion status"() {
		given:
		def mockedDeletePipelineResponse = new MockResponse()
		mockedDeletePipelineResponse.responseCode = 200
		mockedDeletePipelineResponse.body = '''{"data":{"deletePipeline":true}}'''
		mockWebServer.enqueue(mockedDeletePipelineResponse)
		
		buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'test.user'
                    password = 'password'
                    environment = 2
                }

                pipeline {
                    id = "pipeline-id"
                }
            }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		then:
		notThrown(Exception)
		!result.output.contains('"data"')
		result.output.contains('{"status":"success"}')
	}
	
	def "projectsDeletePipeline should fail if no pipeline id is provided"() {
		given:
		buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'test.user'
                    password = 'password'
                    environment = 2
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
	
	def "projectsDeletePipeline should fail if pipeline id doesn't exists"() {
		given:
		def mockedDeletePipelineResponse = new MockResponse()
		mockedDeletePipelineResponse.responseCode = 200
		mockedDeletePipelineResponse.body = '''{"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}'''
		mockWebServer.enqueue(mockedDeletePipelineResponse)
		
		buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'test.user'
                    password = 'password'
                    environment = 2
                }

                pipeline {
                    id = 'bad-id'
                }
            }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		then:
		UnexpectedBuildFailure e = thrown()
		result == null
		e.message.contains('Something went wrong when deleting pipeline: {"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}')
		e.getBuildResult().task(":${taskName}").outcome == FAILED
	}
}
