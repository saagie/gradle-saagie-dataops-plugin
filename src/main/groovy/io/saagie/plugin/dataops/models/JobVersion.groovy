package io.saagie.plugin.dataops.models

class JobVersion {
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
}
