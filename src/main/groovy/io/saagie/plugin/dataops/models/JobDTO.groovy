package io.saagie.plugin.dataops.models

class JobDTO implements IExists{
    String name
    String id
    String description
    String category
    String technology
    Boolean isScheduled
    String cronScheduling
    AlertingDTO alerting= new AlertingDTO()

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
            name = jobDetailResult.name //
            id = jobDetailResult.id
            description = jobDetailResult.description //
            category = jobDetailResult.category //
            technology = jobDetailResult.technology.id
            isScheduled = jobDetailResult.isScheduled
            cronScheduling = jobDetailResult.cronScheduling
            alerting = jobDetailResult.alerting
    }

    void setJobFromV1ApiResult(jobDetailResultV1, technology, technologyVersion, cronScheduling) {
        // TODO Set JOB FROM V1 TO V2
        name = jobDetailResultV1.name
        description = jobDetailResultV1.description
        category = jobDetailResultV1.category

        this.technology = technology


    }

}
