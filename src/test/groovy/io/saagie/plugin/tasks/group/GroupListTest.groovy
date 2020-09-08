package io.saagie.plugin.tasks.group

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.GROUP_LIST_TASK
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('groupList task tests')
class GroupListTest extends DataOpsGradleTaskSpecification {
    String taskName = GROUP_LIST_TASK

    def "the task should return a list of all user groups"() {
        given:
        enqueueRequest("token")
        enqueueRequest("[{\"name\":\"group-0\",\"role\":\"ROLE_USER\",\"authorizations\":[{\"platformId\":4,\"platformName\":\"Demo\",\"permissions\":[]}],\"protected\":false}]")

        buildFile << '''
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 3
                    jwt = true
                }
            }
        '''

        when:
        BuildResult result = gradle(taskName, "d")

        then:
        notThrown(Exception)
        result.output.contains('["group-0"]')
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
