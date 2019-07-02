package io.saagie.plugin

import org.gradle.api.InvalidUserDataException
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.*

import static org.gradle.testkit.runner.TaskOutcome.*

class DataOpsPluginTest extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << 'plugins { id "io.saagie.gradle-saagie-plugin" }\n'
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

    def "hello task should print 'Hello World'"() {
        given:
        def result = gradle('hello')

        expect:
        result.task(":hello").outcome == SUCCESS
        result.output.contains('Hello, world!')
    }

    def "hello task with params must return the appropriate value"() {
        given:
        buildFile << 'saagie.alternativeGreeting = "Howdy"'

        when:
        def result = gradle('hello')

        then:
        result.task(":hello").outcome == SUCCESS
        result.output.contains("Howdy, world!")
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
