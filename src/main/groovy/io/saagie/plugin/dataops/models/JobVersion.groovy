package io.saagie.plugin.dataops.models

class JobVersion implements IMapable {
    String commandLine
    String releaseNote
    String runtimeVersion
    Boolean usePreviousArtifact = false

    List<String> volume = []
    List<ExposedPort> exposedPorts = []

    @Deprecated
    PackageInfo packageInfo = new PackageInfo()

    DockerInfos dockerInfo = new DockerInfos()

    Resources resources = new Resources()

    ExtraTechnology extraTechnology = new ExtraTechnology()

    @Deprecated
    Object packageInfo(Closure closure) {
        packageInfo.with(closure)
    }

    Object dockerInfo(Closure closure) {
        dockerInfo.with(closure)
    }

    Object resources(Closure closure) {
        resources.with(closure)
    }

    Object extraTechnology(Closure closure) {
        extraTechnology.with(closure)
    }

    @Override
    Map toMap() {
        return [
            commandLine        : commandLine,
            releaseNote        : releaseNote,
            runtimeVersion     : runtimeVersion,
            volume             : volume,
            usePreviousArtifact: usePreviousArtifact,
            exposedPorts       : exposedPorts.collect { it.toMap() },
            packageInfo        : packageInfo.toMap(),
            dockerInfo         : dockerInfo.toMap(),
            resources          : resources.toMap(),
            extraTechnology    : extraTechnology.toMap(),
        ]
    }
}
