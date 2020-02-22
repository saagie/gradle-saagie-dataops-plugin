package io.saagie.plugin.dataops.models

class Pipeline implements IMapable {
    String id
    String name
    String description
    def ids = []
    Boolean include_job = false
    Boolean isScheduled = false
    String cronScheduling
    Alerting alerting = new Alerting()

    Object alerting(Closure closure) {
        alerting.with(closure)
    }

    @Override
    Map toMap() {
        def pipelineMap = [
            id            : id,
            description   : description,
            isScheduled   : isScheduled,
            cronScheduling: cronScheduling
        ]
        if (name) {
           pipelineMap.put('name', name)
        }

        if(alerting && alerting.emails){
            pipelineMap << [alerting:alerting.toMap()]
        }
        return pipelineMap
    }
}
