package io.saagie.plugin.dataops.tasks.service.importTask

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.AppMapper
import io.saagie.plugin.dataops.models.AppVersionDTO

class ImportAppService {


    def static importAndCreateApps(Map apps, DataOpsExtension globalConfig, Closure mapClosure) {

        apps.each { app ->
            def versions = null


            AppMapper newMappedAppData = new AppMapper()

            newMappedAppData.app {
                name = app.name
                technology = app.technology
                isScheduled = app.isScheduled
                category = app.category
                description = app.description
                storageSizeInMB = app.storageSizeInMB
            }

            if (app.alerting?.emails) {
                newMappedAppData.app.alerting {
                    emails = app.alerting?.emails
                    statusList = app.alerting?.statusList
                    logins = app.alerting?.logins
                }
            }

            newMappedAppData.appVersion {
                releaseNote = app.appVersion.releaseNote

                dockerInfo {
                    image = app.appVersion.dockerInfo?.image
                    dockerCredentialsId = app.appVersion.dockerInfo?.dockerCredentialsId
                }
                resources {
                    memory = app.appVersion.resources.memory
                    cpu = app.appVersion.resources.cpu
                    disk = app.appVersion.resources.disk
                }
                exposedPorts {
                    name = app.appVersion.exposedPorts.name
                    port = app.appVersion.exposedPorts.port
                    isAuthenticationRequired = app.appVersion.exposedPorts.isAuthenticationRequired
                    isRewriteUrl = app.appVersion.exposedPorts.isRewriteUrl
                    basePathVariableName = app.appVersion.exposedPorts.basePathVariableName
                }

            }

            if (app.versions && app.versions.size() > 0) {
                versions = app.versions
            }


            mapClosure(newMappedAppData, app, app.key, versions, app.technologyName)
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
            exposedPorts {
                name = appVersionMap.exposedPorts.name
                port = appVersionMap.exposedPorts.port
                isAuthenticationRequired = appVersionMap.exposedPorts.isAuthenticationRequired
                isRewriteUrl = appVersionMap.exposedPorts.isRewriteUrl
                basePathVariableName = appVersionMap.exposedPorts.basePathVariableName
            }

        }

        return appVersion
    }

}
