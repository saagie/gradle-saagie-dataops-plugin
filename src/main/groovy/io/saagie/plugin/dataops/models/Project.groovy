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

    List<Closure<TechnologyByCategory>> technologyByCategory = []
    List<Closure<SecurityGroup>> authorizedGroups = []
    boolean include_all_artifacts = false
    boolean has_graph_pipelines = false

    @Override
    Map toMap() {
        if (id) {
            return [
                id                    : id,
                name                  : name,
                creator               : creator,
                description           : description,
                jobsCount             : jobsCount,
                status                : status,
                technologiesByCategory: technologyByCategory ? (
                    technologyByCategory.collect({
                        TechnologyByCategory tech = new TechnologyByCategory()
                        tech.with(it)
                        return tech.toMap()
                    })
                ) : null,
                authorizedGroups      : authorizedGroups.collect({
                    SecurityGroup securityGroup = new SecurityGroup()
                    securityGroup.with(it)
                    return securityGroup.toMap()
                })
            ]
        } else if (!id && name) {
            return [
                name                  : name,
                description           : description,
                technologiesByCategory: technologyByCategory ? (
                    technologyByCategory.collect({
                        TechnologyByCategory tech = new TechnologyByCategory()
                        tech.with(it)
                        return tech.toMap()
                    })
                ) : null,
                authorizedGroups      : authorizedGroups.collect({
                    SecurityGroup securityGroup = new SecurityGroup()
                    securityGroup.with(it)
                    return securityGroup.toMap()
                })
            ]
        }
        return null
    }
}
