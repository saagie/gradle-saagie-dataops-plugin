package io.saagie.plugin.group

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.GROUP_LIST
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('groupList task tests')
class GroupListTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    @Shared MockWebServer mockWebServer = new MockWebServer()
    @Shared String taskName = GROUP_LIST

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

    def "the task should return a list of all user groups"() {
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
        result.output.contains('["group-p4"]')
    }

    def "the task should fail if there is no jwt"() {
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
}
