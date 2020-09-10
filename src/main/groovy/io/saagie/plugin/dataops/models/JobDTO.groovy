package io.saagie.plugin.dataops.models

import io.saagie.plugin.dataops.tasks.service.CategoryService
import org.gradle.api.GradleException

class JobDTO implements IExists {
    String name
    String id
    String description
    String category
    String technology
    Boolean isScheduled
    String cronScheduling
    AlertingDTO alerting = new AlertingDTO()

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
        if (jobDetailResult?.extraTechnology) {
            extraTechnology = jobDetailResult.extraTechnology
        }
    }

    void setJobFromV1ApiResult(jobV1DetailResult, technology, cronScheduling) {

        if (!technology.id) {
            throw GradleException("Technology can t be null when mapped from v1")
        }

        id = jobV1DetailResult.id
        name = jobV1DetailResult.name
        description = jobV1DetailResult.description
        category = CategoryService.instance.getCategoryByV1CategoryAndTechnology(jobV1DetailResult.category, technology.label)
        isScheduled = !jobV1DetailResult.manual

        if (isScheduled) {
            this.cronScheduling = cronScheduling
        }

        this.technology = technology.id
    }

}
