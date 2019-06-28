package io.saagie.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.gradle.testkit.runner.TaskOutcome.*

class DataOpsPluginTest {
    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile

    @Before
    void setup() {
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

    @Test
    void "hello task should print 'Hello World'"() {
        def result = gradle('hello')
        assert result.task(":hello").outcome == SUCCESS
        assert result.output.contains('Hello, world!')
    }

    @Test
    void "hello task with params must return the appropriate value"() {
        buildFile << 'saagie.alternativeGreeting = "Howdy"'

        def result = gradle('hello')

        assert result.task(":hello").outcome == SUCCESS
        assert result.output.contains("Howdy, world!")
    }

    @Test
    void "projectList task must return the appropriate value"() {
        buildFile << """
            saagie {
                server {
                    url = 'https://saagie-beta.prod.saagie.io/'
                    login = 'my-login'
                    password = 'my-password'
                    environment = 4
                }
            }
        """

        def result = gradle('projectList')
        assert result.task(":projectList").outcome == SUCCESS
        assert result.output.contains("Project list")
    }
}
