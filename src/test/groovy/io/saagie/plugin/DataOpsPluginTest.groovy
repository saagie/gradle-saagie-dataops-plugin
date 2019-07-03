package io.saagie.plugin

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.*

class DataOpsPluginTest extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

    MockWebServer mockWebServer = new MockWebServer()

    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << 'plugins { id "io.saagie.gradle-saagie-dataops-plugin" }\n'
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


    // ===========================
    // TESTS =====================
    def "projectList task should return a list of project"() {
        given:
        def mockedResponse = new MockResponse()
        mockedResponse.setResponseCode(200)
        mockedResponse.body = """
            {
                "data": {
                    "projects": [
                        {
                            "id": "8321e13c-8de2a-4481-8552-5be4b6cc5df4",
                            "name": "Test new Project",
                            "creator": "simon.chmel",
                            "description": "Test Simon",
                            "jobsCount": 2,
                            "status": "READY"
                        },
                        {
                            "id": "7f5e0374-0c45-45a3-a2f3-bdlezdezd40f876fa",
                            "name": "Test Spark config",
                            "creator": "test.dezdzek",
                            "description": "",
                            "jobsCount": 1,
                            "status": "READY"
                        },
                        {
                            "id": "dezdzjx-7b7f-44f0-9f69-cd377152a2e9",
                            "name": "For tests",
                            "creator": "test.name",
                            "description": "Pour Test Name",
                            "jobsCount": 7,
                            "status": "READY"
                        },
                        {
                            "id": "5b781e22-dzdioz-4831-8a03-deiozjdiz",
                            "name": "Test R 6.8.9",
                            "creator": "test.test",
                            "description": "",
                            "jobsCount": 0,
                            "status": "READY"
                        }
                    ]
                }
            }
        """
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }
            }
        """
        mockWebServer.enqueue(mockedResponse)
        mockWebServer.start(9000)

        when:
        def result = gradle('projectList')

        then:
        !result.output.contains('"data"')
        result.output.contains('[{"id":"8321e13c')
    }

    def "projectList task with bad config should fail"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = 'https://saagie-beta.prod.saagie.io/'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }
            }
        """

        when:
        def result = gradle('projectList')

        then:
        thrown(Exception)
    }
}
