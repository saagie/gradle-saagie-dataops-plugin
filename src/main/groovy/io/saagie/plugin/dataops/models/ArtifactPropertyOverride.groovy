package io.saagie.plugin.dataops.models

class ArtifactPropertyOverride implements IExists, IMapable {

    Boolean isScheduled = false
    String cronScheduling
    Alerting alerting = new Alerting()

    Object alerting(Closure closure) {
        alerting.with(closure)
    }

    @Override
    boolean exists() {
        return (isScheduled && cronScheduling || alerting.toMap())
    }

    @Override
    Map toMap() {
        if (!exists()) return [:]
        return [
            isScheduled   : isScheduled,
            cronScheduling: cronScheduling,
            alerting      : alerting.toMap(),
        ]
    }
}
