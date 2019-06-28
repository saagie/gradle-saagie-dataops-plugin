package io.saagie.plugin.dataops

import io.saagie.plugin.dataops.models.Server

class DataOpsExtension {
    String alternativeGreeting
    Server server = new Server()

    Object server(Closure closure) {
        server.with(closure)
    }
}
