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
        if(version.commandLine){
            commandLine = version.commandLine
        }
        if(version.dockerInfo && version.dockerInfo.dockerCredentialsId){
            dockerInfo.dockerCredentialsId = version.dockerInfo.dockerCredentialsId
        }
        if(version.dockerInfo && version.dockerInfo.image){
            dockerInfo.image = version.dockerInfo.image
        }
        if(version.runtimeVersion){
            runtimeVersion = version.runtimeVersion
        }
        if(version.releaseNote){
            releaseNote = version.releaseNote
        }
        if(version.packageInfo.downloadUrl){
            packageInfo.downloadUrl = version.packageInfo.downloadUrl
        }
        if(version.packageInfo.name){
            packageInfo.name = version.packageInfo.name
        }
    }
}
