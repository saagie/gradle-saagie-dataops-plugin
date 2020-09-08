package io.saagie.plugin.dataops.models

class SecurityGroup implements IMapable, IExists {

    String id
    String role

    @Override
    Map toMap() {
        if (!exists()) return null
        return [
                name: id,
                role: role
        ]
    }

    @Override
    boolean exists() {
        return id
    }
}
