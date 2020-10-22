package io.saagie.plugin.dataops.models

class AppMapper {
    App job = new App()
    AppVersionDTO jobVersion = new AppVersionDTO()


    Object app(Closure closure) {
        app.with(closure)
    }

    Object appVersion(Closure closure) {
        appVersion.with(closure)
    }

    def static Map mapAppAndAppVersionWithoutMail(App app, AppVersionDTO appVersion, String projectId) {
        if (!app) {
            return null
        }
        def newAppConfig = mapAppWithoutMail(app, projectId)
        def mappedAppVersion = appVersion.toMap()


        if (appVersion) {
            newAppConfig.put('jobVersion', [
                releaseNote   : mappedAppVersion.releaseNote
            ])

            if (mappedAppVersion.dockerInfo?.image) {
                newAppConfig.jobVersion.put('dockerInfo', [
                    image              : mappedAppVersion.dockerInfo?.image,
                    dockerCredentialsId: mappedAppVersion.dockerInfo?.dockerCredentialsId
                ])
            }

            if (mappedAppVersion.resources?.cpu || mappedAppVersion.resources?.disk || mappedAppVersion.resources?.memory) {
                newAppConfig.jobVersion.put('resources', mappedAppVersion.resources)
            }


            if (mappedAppVersion.exposedPorts) {
                newAppConfig.jobVersion.put('exposedPorts', mappedAppVersion.exposedPorts)
            }


        }
        return newAppConfig
    }

    def static mapAppWithoutMail(App app, String projectId) {
        def mappedApp = app.toMap()
        def newAppConfig = [:]

        def technology = app.technology


        newAppConfig.put('job', [
            name          : mappedApp?.name,
            isScheduled   : mappedApp?.isScheduled,
            category      : mappedApp?.category,
            isStreaming   : mappedApp?.isStreaming,
            description   : mappedApp?.description,
            technology    : [id: technology],
            storageSizeInMB: mappedApp?.storageSizeInMB
        ])

        if (projectId) {
            newAppConfig.job.put("projectId", projectId)
        }

        if (mappedApp.id) {
            newAppConfig.job.put('id', mappedApp.id)
        }

        if (mappedApp.alerting?.emails) {
            newAppConfig.job.put('alerting', [
                emails    : mappedApp.alerting?.emails,
                statusList: mappedApp.alerting?.statusList,
                logins    : mappedApp.alerting?.logins
            ])
        }
        return newAppConfig
    }
}
