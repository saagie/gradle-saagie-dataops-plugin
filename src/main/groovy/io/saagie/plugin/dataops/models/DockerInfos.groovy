package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class DockerInfos implements IMapable, IExists {
    String image
    String dockerCredentialsId

    @Override
    Map toMap() {
        if (exists()) {
            return [
                image : image,
                dockerCredentialsId: dockerCredentialsId,
            ]
        }
        return null
    }

    @Override
    boolean exists() {
        return image
    }

    @Override
    boolean equals(o) {
        println "equals(o) triggered"
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        DockerInfos dockerInfos = (DockerInfos) o

        if (image != dockerInfos.image) return false
        if (dockerCredentialsId != dockerInfos.dockerCredentialsId) return false

        return true
    }
}
