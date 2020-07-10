package io.saagie.plugin.dataops.models
import org.gradle.api.GradleException
import org.jetbrains.annotations.NotNull

class JobVersionDTO implements IExists, Comparable{
    String commandLine
    DockerInfos dockerInfo = new DockerInfos()
    String runtimeVersion
    String releaseNote
    ExtraTechnology extraTechnology
    PackageInfo packageInfo = new PackageInfo()
    String number

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
    
        if(current.number) {
            number = current.number
        }
        
        if(version.packageInfo?.name){
            packageInfo.name = version.packageInfo?.name
        }

    }

    void setJobVersionFromV1ApiResult(technologyV2container, current) {
        if(!technologyV2container.version2){
            throw new GradleException("technologyV2container doesn't contain critical data information")
        }
        def version2 = technologyV2container.version2
        def extraTechnologyData = technologyV2container.extraTechnology
        if(extraTechnologyData) {
            extraTechnology = extraTechnologyData
        }
        
        if(current.number) {
            number = current.number
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
    
        if ( current.releaseNote ) {
            releaseNote = current.releaseNote
        }
        
        if(current.file) {
            packageInfo.name = current.file
        }

        if(current.number) {
            number = current.number
        }

        if(runtimeTechnologyVersion) {
            runtimeVersion = runtimeTechnologyVersion
        }

        if(current.releaseNote){
            releaseNote = current.releaseNote
        }
    }

    @Override
    int compareTo(@NotNull Object o) {

        commandLine <=> o.commandLine?:
            releaseNote <=> o.releaseNote?:
                !o.dockerInfo.equals(dockerInfo) ? 1:
                    !o.extraTechnology.equals(extraTechnology) ? 1:
                        !o.packageInfo.equals(packageInfo) ? 1 : 0
    }
}
