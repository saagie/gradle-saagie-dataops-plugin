package io.saagie.plugin.dataops.models

class JobVersionDTO implements IExists{
    String commandLine
    DockerInfos dockerInfo = new DockerInfos()
    String runtimeVersion
    String releaseNote
    PackageInfo packageInfo = new PackageInfo()

    @Override
    boolean exists() {
        return commandLine ||
            dockerInfo ||
            runtimeVersion ||
            releaseNote ||
            packageInfo
    }

    void setJobVersionFromApiResult(version) {
        commandLine = version.commandLine
        dockerInfo = version.dockerInfo
        runtimeVersion = version.runtimeVersion
        releaseNote = version.releaseNote
        packageInfo = version.packageInfo
    }
}
