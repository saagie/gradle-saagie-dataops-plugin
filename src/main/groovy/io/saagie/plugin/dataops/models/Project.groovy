package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class Project implements IMapable {
    String id
    String name
    String creator
    String description
    Integer jobsCount
    String status

    private interface ITechnologyByCategory {
        String category
        List<String> technologyid
    }

    List<Closure<TechnologyByCategory>> technologyByCategory = []
    List<SecurityGroup> authorizedGroups = []

    @Override
    Map toMap() {
        if (id && name && creator && description && jobsCount && status) {
            return [
                id         : id,
                name       : name,
                creator    : creator,
                description: description,
                jobsCount  : jobsCount,
                status     : status
            ]
        } else if (!id && name) {
            return [
                name: name,
                description: description,
                technologiesByCategory: technologyByCategory ? (
                    technologyByCategory.collect({
                        TechnologyByCategory tech = new TechnologyByCategory()
                        tech.with(it)
                        return tech.toMap()
                    })
                ) : null,
                authorizedGroups: authorizedGroups.collect({ it.toMap() })
            ]
        }
        return null
    }
}
