package io.saagie.plugin.tasks.platform

import io.saagie.plugin.DataOpsGradleTaskSpecification
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.utils.SaagieUtils
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PLATFORM_LIST_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('platformList task tests')
class PlatformListTaskTests extends DataOpsGradleTaskSpecification {
	@Shared
	String taskName = PLATFORM_LIST_TASK
	
	def "platformList task should list platforms"() {
		given:
		def mockedJwtAuth = new MockResponse()
		mockedJwtAuth.responseCode = 200
		mockedJwtAuth.body = 'token'
		mockWebServer.enqueue(mockedJwtAuth)
		
		def mockedResponse = new MockResponse()
		mockedResponse.responseCode = 200
		mockedResponse.body = """{"login":"fake.user","groups":["group-p4"],"roles":["ROLE_USER"],"authorizations":[{"platformId":4,"platformName":"Demo","permissions":[{"artifact":{"type":"DATAGOV"},"role":"ROLE_MANAGER"},{"artifact":{"type":"DATAFABRIC"},"role":"ROLE_ACCESS"},{"artifact":{"type":"PROJECTS_CREATOR"},"role":"ROLE_PROJECT_CREATOR"},{"artifact":{"type":"PROJECTS"},"role":"ROLE_PROJECT_MANAGER"},{"artifact":{"type":"PROJECTS_ENVVAR_EDITOR"},"role":"ROLE_PROJECT_ENVVAR_EDITOR"},{"artifact":{"type":"DATASET_ACCESSES"},"role":"ROLE_READ_WRITE"},{"artifact":{"type":"DATASET_ACCESS_MANAGER"},"role":"ROLE_MANAGER"},{"artifact":{"type":"DATA_API"},"role":"ROLE_ACCESS"}]}]}"""
		mockWebServer.enqueue(mockedResponse)
		
		buildFile << '''
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    jwt = true
                }
            }
        '''
		
		when:
		BuildResult result = gradle(taskName)
		
		then:
		notThrown(Exception)
		result.output.contains('"platformId"')
		result.output.contains('"platformName"')
	}
	
	def "platformList task should fail if the jwt option is not provided"() {
		given:
		buildFile << '''
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
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
	
	def "platformList task should use a realm"() {
		DataOpsExtension configuration = new DataOpsExtension()
		configuration.server {
			url = 'http://localhost:9000'
			login = 'user'
			password = 'password'
			jwt = true
			realm = 'UserRealm' // for test only
		}
		SaagieUtils saagieUtils = new SaagieUtils(configuration)
		
		when:
		Request req = saagieUtils.getPlatformListRequest()
		
		then:
		req.header('Cookie') != null
		req.header('Cookie').contains("SAAGIETOKEN${configuration.server.realm.toUpperCase()}")
	}
	
	def "platformList should have a Saagie-Realm header set to the lowered cased realm value"() {
		given:
		DataOpsExtension configuration = new DataOpsExtension()
		configuration.server {
			url = 'http://localhost:9000'
			login = 'user'
			password = 'password'
			jwt = true
			realm = 'UserRealm' // For test
		}
		SaagieUtils saagieUtils = new SaagieUtils(configuration)
		
		when:
		Request req = saagieUtils.getPlatformListRequest()
		
		then:
		req.header('Saagie-Realm') == configuration.server.realm.toLowerCase()
	}
}
