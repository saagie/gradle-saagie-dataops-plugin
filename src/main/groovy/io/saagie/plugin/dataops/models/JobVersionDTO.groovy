package io.saagie.plugin.dataops.models

import org.gradle.api.GradleException

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

        if(version.dockerInfo && version.dockerInfo.dockerCredentialsId){
            dockerInfo.dockerCredentialsId = version.dockerInfo.dockerCredentialsId
        }

        if(version.dockerInfo && version.dockerInfo.image){
            dockerInfo.image = version.dockerInfo.image
        }

        commandLine = version.commandLine
        runtimeVersion = version.runtimeVersion
        releaseNote = version.releaseNote

        if(version.packageInfo?.name){
            packageInfo.name = version.packageInfo?.name
        }

    }

    void setJobVersionFromV1ApiResult(versionV1, technologyVersion, current) {

        if(!current) {
            throw GradleException("Current can't be null from version V1")
        }

        if(current.template) {
            commandLine = current.template
        }

        if(current.packageUrl) {
            dockerInfo.image = current.packageUrl
        }

        if(current.file) {
            packageInfo.name = current.file
        }

        if(technologyVersion) {
            runtimeVersion = technologyVersion
        }

        if(current.releaseNote){
            releaseNote = current.releaseNote
        }
    }


}
