package io.saagie.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
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
                id 'io.saagie.plugin.DataOpsPlugin'
            }
        """

        pluginClasspath = getClass().classLoader
            .findResource('plugin-classpath.txt')
            .readLines()
            .collect {
                new File(it)
            }
    }

    def "hello task should print 'Hello World'"() {
        when:
            buildFile << """
            """
            Project project = ProjectBuilder.builder().build()
            project.pluginManager.apply(DataOpsPlugin.class)

        then:
            println(project.tasks.getByName('hello'))
    }

    def "projectList task should print 'Project List'"() {
        when:
            buildFile << """
            """
            Project project = ProjectBuilder.builder().build()
            project.pluginManager.apply(DataOpsPlugin.class)

        then:
            println(project.tasks.getByName('projectList'))
    }
}
