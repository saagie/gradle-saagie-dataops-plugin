package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExtraTechnology implements IMapable, IExists {
    String language
    String version

    @Override
    Map toMap() {
        if (exists()) {
            return [
                language: language,
                version : version
            ]
        }
        return null
    }

    @Override
    boolean exists() {
        return language && version
    }
}
