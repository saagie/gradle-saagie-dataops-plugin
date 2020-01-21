package io.saagie.plugin.dataops.clients

import groovy.json.JsonSlurper

class SaagieClientUtils {
    static final EXPORTED_JOB_CONFIG_FILENAME = 'job.json'
    static final EXPORTED_JOB_PACKAGE_FOLDER_NAME = 'package'

    /**
     * run through all files in the provided folder, and return
     * the needed config. (job.json ovewrite infos, and package file to upload)
     *
     * The builded object will looks as the following
     *
     * [
     *     jobs: [
     *         "ae12f-efz123j-23hj": [
     *             configOverride: [],
     *             package: File("path/to/extracted/package")
     *         ]
     *     ]
     * ]
     *
     * @param exportedJobFolder
     * @return Map
     */
    def static extractJobConfigAndPackageFromExportedJob(File exportedJobFolder) {
        Map extractedConfig = [
            jobs: [:],
        ]

        File jobsFolder = new File("${exportedJobFolder.absolutePath}/Job")
        jobsFolder.eachFile { jobFolder ->
            String jobId = jobFolder.name
            String jobFolderPath = jobFolder.absolutePath

            extractedConfig.jobs[jobId] = [
                configOverride: null,
                package: null
            ]

            def jsonParser = new JsonSlurper()
            File jobConfigFile = new File("${jobFolderPath}/${EXPORTED_JOB_CONFIG_FILENAME}")
            extractedConfig.jobs[jobId].configOverride = jsonParser.parse(jobConfigFile)

            def packageFile = new File("${jobFolderPath}/${EXPORTED_JOB_PACKAGE_FOLDER_NAME}").listFiles().head()
            extractedConfig.jobs[jobId].package = packageFile
        }

        return extractedConfig
    }
}
