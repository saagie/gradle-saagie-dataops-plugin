package io.saagie.plugin.dataops.models

class Pipeline implements IMapable {
    String id = null
    String name
    String description = null
    Boolean isScheduled = false
    String cronScheduling = null
    Alerting alerting = new Alerting()

    Object alerting(Closure closure) {
        alerting.with(closure)
    }

    @Override
    Map toMap() {
        if (name) {
            return [
                id: id,
                name: name,
                description: description,
                isScheduled: isScheduled,
                cronScheduling: cronScheduling,
                alerting: alerting.toMap(),
            ]
        }
        return null
    }
}
