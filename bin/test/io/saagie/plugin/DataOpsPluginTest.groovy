package io.saagie.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class DataOpsPluginTest extends Specification {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    List pluginClasspath

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'io.saagie.plugin'
            }
        """

        pluginClasspath = getClass().classLoader
            .findResource('plugin-classpath.txt')
            .readLines()
            .collect {
                new File(it)
            }
    }

    def "can successfully print 'Hello World'"() {
        buildFile << """
            verification {
                url = 'https://www.google.com/'
            }
        """

        when:
            def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('hello')
                .withPluginClasspath()
                .build()

        then:
            result.output.contains("Hello World")
            result.task(":hello").outcome == SUCCESS
    }

    def "hello task should print 'Hello World'"() {
        given:
            buildFile << """
            """
            Project project = ProjectBuilder.builder().build()
            project.pluginManager.apply(DataOpsPlugin.class)

        when:
            def taskResult = project.tasks.getByName('hello')

        then:
            println(taskResult)
    }
}
