package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class JobVersion implements IMapable, IExists {
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
        if (exists()) {
            return [
                commandLine        : commandLine,
                releaseNote        : releaseNote,
                runtimeVersion     : runtimeVersion,
                volume             : volume ? volume : null,
                usePreviousArtifact: usePreviousArtifact,
                exposedPorts       : exposedPorts ? exposedPorts.collect({ it.toMap() }) : null,
                dockerInfo         : dockerInfo.toMap(),
                resources          : resources.toMap(),
                extraTechnology    : extraTechnology.toMap(),
            ]
        }
        return null
    }

    @Override
    boolean exists() {
        return (
            commandLine ||
            releaseNote ||
            runtimeVersion ||
            usePreviousArtifact ||

            // NOTE: empty lists evaluate to false in groovy
            volume ||

            // Check that exposedPorts isn't empty and that it contains valid ExposedPorts
            exposedPorts && exposedPorts.every { it.exists() } ||
            packageInfo.exists() ||
            dockerInfo.exists() ||

            // TODO: uncomment this check once it will be available in the API
            // resources.exists() ||
            extraTechnology.exists()
        )
    }
}
