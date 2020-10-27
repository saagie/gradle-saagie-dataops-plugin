package io.saagie.plugin.dataops.tasks.service.importTask

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.AppMapper
import io.saagie.plugin.dataops.models.AppVersionDTO

class ImportAppService {


    def static importAndCreateApps(Map apps, DataOpsExtension globalConfig, Closure mapClosure) {

        apps.each { app ->
            def versions = null
            def appId = app.key
            Map appConfig = app.value.configOverride

            AppMapper newMappedAppData = new AppMapper()

            newMappedAppData.job {
                name = appConfig.app.name
                technology = appConfig.app.technology
                isScheduled = appConfig.app.isScheduled
                category = appConfig.app.category
                description = appConfig.app.description
                storageSizeInMB = appConfig.app.storageSizeInMB
                isStreaming = appConfig.app.isStreaming
            }

            if (appConfig.app.alerting?.emails) {
                newMappedAppData.job.alerting {
                    emails = appConfig.app.alerting?.emails
                    statusList = appConfig.app.alerting?.statusList
                    logins = appConfig.app.alerting?.logins
                }
            }

            newMappedAppData.jobVersion {
                releaseNote = appConfig.appVersion.releaseNote

                dockerInfo {
                    image = appConfig.appVersion.dockerInfo?.image
                    dockerCredentialsId = appConfig.appVersion.dockerInfo?.dockerCredentialsId
                }

                resources {
                    memory = appConfig.appVersion.resources.memory
                    cpu = appConfig.appVersion.resources.cpu
                    disk = appConfig.appVersion.resources.disk
                }

                exposedPorts = appConfig.appVersion.exposedPorts

            }

            if (appConfig.versions && appConfig.versions.size() > 0) {
                versions = appConfig.versions
            }


            mapClosure(newMappedAppData, app, app.key, versions, appConfig.app.technologyName)
        }

    }

    static convertFromMapToJsonVersion(appVersionMap) {
        AppVersionDTO appVersion = []
        appVersion.with {
            releaseNote = appVersionMap.releaseNote

            dockerInfo {
                image = appVersionMap.dockerInfo?.image
                dockerCredentialsId = appVersionMap.dockerInfo?.dockerCredentialsId
            }
            resources {
                memory = appVersionMap.resources.memory
                cpu = appVersionMap.resources.cpu
                disk = appVersionMap.resources.disk
            }

            exposedPorts = appVersionMap.exposedPorts

        }

        return appVersion
    }

}
