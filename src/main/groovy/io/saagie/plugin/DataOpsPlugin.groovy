package io.saagie.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class DataOpsPluginExtension {
    String name = 'World'
}

class DataOpsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def extension = project.extensions.create('saagie', DataOpsPluginExtension)
        project.task('hello') {
            doLast {
                println "Hello ${extension.name}"
                "Hello ${extension.name}"
            }
        }
    }
}
