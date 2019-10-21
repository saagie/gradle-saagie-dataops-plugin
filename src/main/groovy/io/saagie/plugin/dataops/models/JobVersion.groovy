package io.saagie.plugin.dataops.models

class JobVersion implements IMapable {
    String commandLine
    String releaseNote
    String runtimeVersion
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
        if ((commandLine && releaseNote && runtimeVersion) ||
            packageInfo.toMap() || dockerInfos.toMap() || resources.toMap()
        ) {
            return [
                commandLine: commandLine,
                releaseNote: releaseNote,
                runtimeVersion: runtimeVersion,
                packageInfo: packageInfo.toMap(),
                dockerInfos: dockerInfos.toMap(),
                resources: resources.toMap(),
            ]
        }
        return null;
    }
}
