package io.saagie.plugin.dataops.models

class JobDTO implements IExists{
    String name
    String id
    String description
    String category
    String technology
    Boolean isScheduled
    String cronScheduling
    Alerting alerting= new Alerting()

    Object alerting(Closure closure) {
            alerting.with(closure)
    }

    @Override
    boolean exists() {
        return name ||
            id ||
            description ||
            category ||
            technology ||
            isScheduled ||
            cronScheduling
    }

    void setJobFromApiResult(jobDetailResult) {
            name = jobDetailResult.name
            id = jobDetailResult.id
            description = jobDetailResult.description
            category = jobDetailResult.category
            technology = jobDetailResult.technology
            isScheduled = jobDetailResult.isScheduled
            cronScheduling = jobDetailResult.cronScheduling
            alerting = jobDetailResult.alerting

    }
}
