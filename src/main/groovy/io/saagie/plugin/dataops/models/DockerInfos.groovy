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
}
