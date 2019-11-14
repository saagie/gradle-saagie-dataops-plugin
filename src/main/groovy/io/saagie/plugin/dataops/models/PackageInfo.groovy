package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class PackageInfo implements IMapable, IExists {
    String name

    @Override
    Map toMap() {
        if (exists()) {
            return [name: name]
        }
        return null
    }

    @Override
    boolean exists() {
        return name
    }
}
