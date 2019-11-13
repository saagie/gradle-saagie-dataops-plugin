package io.saagie.plugin.platform

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.utils.SaagieUtils
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Ignore
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('platformList task tests')
class PlatformListTaskTests extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    @Shared MockWebServer mockWebServer = new MockWebServer()
    @Shared String taskName = 'platformList'

    File buildFile
    File jobFile

    def setupSpec() {
        mockWebServer.start(9000)
    }

    def cleanupSpec() {
        mockWebServer.shutdown()
    }

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << 'plugins { id "io.saagie.gradle-saagie-dataops-plugin" }\n'

        jobFile = testProjectDir.newFile('jobFile.py')
    }

    def cleanup() {
        mockWebServer.dispatcher.peek()
    }

    private BuildResult gradle(boolean isSuccessExpected, String[] arguments = ['tasks']) {
        arguments += '--stacktrace'
        def runner = GradleRunner.create()
            .withArguments(arguments)
            .withProjectDir(testProjectDir.root)
            .withPluginClasspath()
            .withDebug(true)

        return isSuccessExpected ? runner.build() : runner.buildAndFail();
    }

    private BuildResult gradle(String[] arguments = ['tasks']) {
        gradle(true, arguments)
    }

    def "platformList task should list platforms"() {
        given:
        def mockedJwtAuth = new MockResponse()
        mockedJwtAuth.responseCode = 200
        mockedJwtAuth.body = """token"""
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
                    realm = 'saagie'
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
