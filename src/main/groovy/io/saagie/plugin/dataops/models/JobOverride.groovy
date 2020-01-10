package io.saagie.plugin.dataops.models

/**
 * Provides an ability to override values thats in export.
 * This provides a flexible option to set environment specific values.
 * The values thats currently supported are isScheduled, cronScheduling, alerting.
 */
class JobOverride implements IExists, IMapable {

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
        if (!exists()) return null
        return [
            isScheduled: isScheduled,
            cronScheduling: cronScheduling,
            alerting: alerting.toMap(),
        ]
    }
}
