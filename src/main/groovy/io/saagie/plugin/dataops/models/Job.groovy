package io.saagie.plugin.dataops.models

class Job {
    String name
    String projectId
    String description
    String category
    String technology
    Boolean isScheduled = false
    Boolean isStreaming = false
    String cronScheduling
    Alerting alerting = new Alerting()

    Object alerting(Closure closure) {
        alerting.with(closure)
    }
}
