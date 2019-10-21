package io.saagie.plugin.dataops.models

class Pipeline implements IMapable {
    String id
    String name
    String description
    Boolean isScheduled = false
    String cronScheduling
    Alerting alerting = new Alerting()

    Object alerting(Closure closure) {
        alerting.with(closure)
    }

    @Override
    Map toMap() {
        if (name) {
            return [
                id            : id,
                name          : name,
                description   : description,
                isScheduled   : isScheduled,
                cronScheduling: cronScheduling,
                alerting      : alerting.toMap(),
            ]
        }
        return null
    }
}
