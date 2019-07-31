package io.saagie.plugin.dataops.models

class Pipeline {
    String name
    String description = null
    Boolean isScheduled = false
    String cronScheduling = null
    Alerting alerting = new Alerting()

    Object alerting(Closure closure) {
        alerting.with(closure)
    }
}
