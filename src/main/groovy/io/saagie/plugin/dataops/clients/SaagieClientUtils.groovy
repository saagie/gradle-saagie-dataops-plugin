package io.saagie.plugin.dataops.clients

class SaagieClientUtils {
    static final EXPORTED_JOB_CONFIG_FILENAME = 'job.json'
    static final EXPORTED_JOB_PACKAGE_FOLDER_NAME = 'package'

    def static extractJobConfigAndPackageFromExportedZip(File exportedJobFolder) {
        def extractedConfig = [
            job: null,
            package: null
        ]

        // TODO: for each file, check if it is the package and job.json and take them

        return extractedConfig
    }
}
