package io.saagie.plugin.dataops.models

class SecurityGroup implements IMapable, IExists {

    String name

    String projectRole

    @Override
    Map toMap() {
        if (!exists()) return null
        return [
            name: name,
            projectRole: projectRole
        ]
    }

    @Override
    boolean exists() {
        return name
    }
}
