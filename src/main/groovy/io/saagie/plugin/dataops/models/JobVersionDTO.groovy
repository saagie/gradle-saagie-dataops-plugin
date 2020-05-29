package io.saagie.plugin.dataops.models

import io.saagie.plugin.dataops.utils.SaagieUtils
import org.gradle.api.GradleException

class JobVersionDTO implements IExists{
    String commandLine
    DockerInfos dockerInfo = new DockerInfos()
    String runtimeVersion
    String releaseNote
    ExtraTechnology extraTechnology
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

    void setJobVersionFromV1ApiResult(versionV1, technologyV2container, current) {
        if(!technologyV2container.version2){
            throw new GradleException("technologyV2container doesn't contain critical data information")
        }
        def version2 = technologyV2container.version2
        def extraTechnologyData = technologyV2container.extraTechnology
        if(extraTechnologyData) {
            extraTechnology = extraTechnologyData
        }
        def runtimeTechnologyVersion =  version2 && version2.versionLabel ?
            version2.versionLabel : null
        if(!current) {
            throw new GradleException("Current can't be null from version V1")
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

        if(runtimeTechnologyVersion) {
            runtimeVersion = runtimeTechnologyVersion
        }

        if(current.releaseNote){
            releaseNote = current.releaseNote
        }
    }


}
