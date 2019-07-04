package io.saagie.plugin

import io.saagie.plugin.dataops.DataOpsModule
import org.gradle.api.Plugin
import org.gradle.api.Project

class DataOpsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        DataOpsModule.load(project);
    }
}
