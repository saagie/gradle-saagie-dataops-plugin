package io.saagie.plugin.dataops

import io.saagie.plugin.dataops.models.Project
import io.saagie.plugin.dataops.models.Server

class DataOpsExtension {
    Server server = new Server()
    Project project = new Project()

    Object server(Closure closure) {
        server.with(closure)
    }

    Object project(Closure closure) {
        project.with(closure)
    }
}
