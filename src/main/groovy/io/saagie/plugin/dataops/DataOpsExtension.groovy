package io.saagie.plugin.dataops

import io.saagie.plugin.dataops.models.Server

class DataOpsExtension {
    Server server = new Server()

    Object server(Closure closure) {
        server.with(closure)
    }
}
