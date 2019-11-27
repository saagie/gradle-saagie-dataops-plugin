package io.saagie.plugin.dataops.models

import io.saagie.plugin.dataops.tasks.projects.TechnologyListTask

class TechnologyByCategory implements IMapable, IExists {
    String jobCategory
    List<String> technologyid = []

    @Override
    Map toMap() {
        if (!exists()) return null
        return [
            jobCategory: jobCategory,
            technologies: technologyid ? technologyid.collect({ [id: it] }) : null,
        ]
    }

    @Override
    boolean exists() {
        return jobCategory
    }
}
