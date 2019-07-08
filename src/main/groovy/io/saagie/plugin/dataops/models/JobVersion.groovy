package io.saagie.plugin.dataops.models

class JobVersion {
    String commandLine
    String releaseNote
    String runtimeVersion
    PackageInfo packageInfo = new PackageInfo()

    PackageInfo packageInfos(Closure closure) {
        packageInfos.with(closure)
    }
}
