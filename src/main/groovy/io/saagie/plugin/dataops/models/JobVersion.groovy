package io.saagie.plugin.dataops.models

class JobVersion implements IMapable {
    String commandLine
    String releaseNote
    String runtimeVersion
    Boolean usePreviousArtifact = false

    List<String> volume = []
    List<ExposedPort> exposedPorts = []

    PackageInfo packageInfo = new PackageInfo()
    DockerInfos dockerInfos = new DockerInfos()
    Resources resources = new Resources()

    Object packageInfo(Closure closure) {
        packageInfo.with(closure)
    }

    Object dockerInfos(Closure closure) {
        dockerInfos.with(closure)
    }

    Object resources(Closure closure) {
        resources.with(closure)
    }

    @Override
    Map toMap() {
        return [
            commandLine        : commandLine,
            releaseNote        : releaseNote,
            runtimeVersion     : runtimeVersion,
            volume             : volume,
            usePreviousArtifact: usePreviousArtifact,
            exposedPorts       : exposedPorts,
            packageInfo        : packageInfo.toMap(),
            dockerInfos        : dockerInfos.toMap(),
            resources          : resources.toMap(),
        ]
    }
}
