package io.saagie.plugin.tasks.job

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_UPGRADE_JOB_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('projectsUpgradeJob task tests')
class JobUpgradeTaskTests extends DataOpsGradleTaskSpecification {
	@Shared
	String taskName = PROJECTS_UPGRADE_JOB_TASK
	
	def "projectsUpgradeJob should update the specified job with only job config"() {
		given:
		enqueueRequest('{"data":{"editJob":{"id":"jobId"}}}') ;
		enqueueRequest('{"data":{"job":{"versions":[{"number":3,"isCurrent":true},{"number":2,"isCurrent":false},{"number":1,"isCurrent":false}]}}}')
		
		
		buildFile << '''
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.user'
                    password = 'password'
                    environment = 2
                }

                job {
                    id = 'jobId'
                    name = 'Updated from gradle'
                    description = 'updated description'
                }
            }
        '''
		
		when:
		BuildResult result = gradle(taskName)
		
		then:
		notThrown(Exception)
		result.output.contains('{"status":"success","version":"3"}')
	}
	
	def "projectsUpgradeJob should fail if job id is missing"() {
		given:
		buildFile << '''
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.user'
                    password = 'password'
                    environment = 2
                }

                job {
                    name = 'Updated from gradle'
                    description = 'updated description'
                    alerting {
                        emails = ['email@email.com']
                        statusList = ['REQUESTED']
                    }
                }
            }
        '''
		
		when:
		BuildResult result = gradle(taskName)
		
		then:
		UnexpectedBuildFailure e = thrown()
		result == null
		e.message.contains("Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/${taskName}")
		e.getBuildResult().task(":${taskName}").outcome == FAILED
	}
	
	def "projectsUpgradeJob should add a new job version and upload script if config is provided"() {
		given:
		enqueueRequest('{"data":{"editJob":{"id":"jobId"}}}')
		enqueueRequest('{"data":{"addJobVersion":{"number":"2"}}}')
		
		jobFile << 'println("Hello gradle")'
		buildFile << """
            saagie {
                server {
                    url = '${mockServerUrl}'
                    login = 'user.user'
                    password = 'password'
                    environment = 4
                }

                job {
                    id = 'jobId'
                }

                jobVersion {
                    runtimeVersion = "3.6"
                    packageInfo {
                        name = "${jobFile.absolutePath}"
                    }
                }
            }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		then:
		result.output.contains('{"status":"success","version":"2"}')
	}
	
	
	def "projectsUpgradeJob should return current job version and update job if current no job version provided"() {
		given:
		enqueueRequest('{"data":{"editJob":{"id":"jobId"}}}')
		enqueueRequest('{"data":{"job":{"versions":[{"number":3,"isCurrent":true},{"number":2,"isCurrent":false},{"number":1,"isCurrent":false}]}}}')
		
		buildFile << """
            saagie {
                server {
                    url = '${mockServerUrl}'
                    login = 'user.user'
                    password = 'password'
                    environment = 4
                }

                job {
                    id = 'jobId'
                    description = 'updated description'
                }
            }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		then:
		result.output.contains('{"status":"success","version":"3"}')
	}
	
	
	def "projectsUpgradeJob should fail if jobVersion is provided without a runtimeVersion"() {
		given: "Build file without jobVersion.runtimeVersion"
		buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'user.user'
                    password = 'password'
                    environment = 4
                }

                job {
                    id = 'jobId'
                }

                jobVersion {
                    releaseNote = 'test release note'
                }
            }
        """
		
		when:
		"gradle ${taskName}"
		BuildResult result = gradle(taskName)
		
		then: "Expect an error to be thrown, and a link to the corresponding task doc"
		UnexpectedBuildFailure e = thrown()
		result == null
		e.message.contains("Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/${taskName}")
		e.getBuildResult().task(":${taskName}").outcome == FAILED
	}
}
