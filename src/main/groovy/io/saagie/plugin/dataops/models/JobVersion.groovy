package io.saagie.plugin.dataops.models

class JobVersion {
    String commandLine
    String releaseNote
    String runtimeVersion
    PackageInfo packageInfo = new PackageInfo()

    Object packageInfos(Closure closure) {
        packageInfos.with(closure)
    }
}
