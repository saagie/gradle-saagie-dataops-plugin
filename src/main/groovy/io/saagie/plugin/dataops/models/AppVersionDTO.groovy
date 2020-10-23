package io.saagie.plugin.dataops.models

class AppVersionDTO implements IExists {
    List<String> storagePaths = []
    String releaseNote
    int number
    List<ExposedPort> exposedPorts = []
    DockerInfos dockerInfo = new DockerInfos()
    Resources resources = new Resources()

    Object dockerInfo(Closure closure) {
        dockerInfo.with(closure)
    }

    Object resources(Closure closure) {
        resources.with(closure)
    }

    Object exposedPorts(Closure closure) {
        exposedPorts.with(closure)
    }



    @Override
    boolean exists() {
        return dockerInfo ||
            releaseNote ||
            resources
    }

    void setAppVersionFromApiResult(version) {

        if (version.dockerInfo && version.dockerInfo.dockerCredentialsId) {
            dockerInfo.dockerCredentialsId = version.dockerInfo.dockerCredentialsId
        }

        if (version.dockerInfo && version.dockerInfo.image) {
            dockerInfo.image = version.dockerInfo.image
        }

        releaseNote = version.releaseNote
        storagePaths = version.storagePaths

        if (version.number) {
            number = version.number
        }

        if (version.exposedPorts) {
            exposedPorts = version.exposedPorts
        }

        if (version.resources) {
            resources = version.resources
        }
    }
}
