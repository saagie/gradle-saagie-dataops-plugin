package io.saagie.plugin.dataops.models

class Project implements IMapable {
    String id
    String name
    String creator
    String description
    Integer jobsCount
    String status

    List<TechnologyByCategory> technologyByCategory = []
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
                technologiesByCategory: technologyByCategory ? technologyByCategory.collect({ it.toMap() }) : null,
                authorizedGroups: authorizedGroups.collect({ it.toMap() })
            ]
        }
        return null
    }
}
