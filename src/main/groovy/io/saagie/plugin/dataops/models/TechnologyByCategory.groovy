package io.saagie.plugin.dataops.models

class TechnologyByCategory implements IMapable, IExists {
    String category
    List<String> technologyid = []

    @Override
    Map toMap() {
        if (!exists()) return null
        return [
            jobCategory: category,
            technologies: technologyid.collect({ [id: it] }),
        ]
    }

    @Override
    boolean exists() {
        return category && !technologyid.empty
    }
}
