package io.saagie.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.*

class DataOpsPluginTest extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

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
        println result.output

        then:
        thrown(Exception)
    }
}
