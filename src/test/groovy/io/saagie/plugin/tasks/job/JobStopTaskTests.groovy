package io.saagie.plugin.tasks.job

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_STOP_JOB_INSTANCE_TASK

@Title('projectsStopJobInstance task tests')
class JobStopTaskTests extends DataOpsGradleTaskSpecification {
	@Shared
	String taskName = PROJECTS_STOP_JOB_INSTANCE_TASK
	
	def "projectsStopJobInstance should stop a job"() {
		given:
		def mockedStopJobResponse = new MockResponse()
		mockedStopJobResponse.responseCode = 200
		mockedStopJobResponse.body = '''{"data":{"stopJobInstance":{"id":"stopped-job-id"}}}'''
		mockWebServer.enqueue(mockedStopJobResponse)
		
		buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.user'
                    password = 'password'
                    environment = 4
                }

                jobinstance {
                    id = 'job-instance-id'
                }
            }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		then:
		result.output.contains('{"status":"success"}')
	}
	
	def "projectsStopJobInstance should fail if job instance id is missing"() {
		given:
		buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.user'
                    password = 'password'
                    environment = 4
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
	
	def "projectsStopJobInstance should fail if job instance doesn't exists"() {
		given:
		def mockedRunJobResponse = new MockResponse()
		mockedRunJobResponse.responseCode = 200
		mockedRunJobResponse.body = '''{"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}'''
		mockWebServer.enqueue(mockedRunJobResponse)
		
		buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'test.user'
                    password = 'password'
                    environment = 2
                }

                jobinstance {
                    id = 'bad-id'
                }
            }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		then:
		UnexpectedBuildFailure e = thrown()
		result == null
		e.message.contains('Something went wrong when stopping the job instance: {"data":null,"errors":[{"message":"Unexpected error","extensions":null,"path":null}]}')
		e.getBuildResult().task(":${taskName}").outcome == FAILED
	}
}
