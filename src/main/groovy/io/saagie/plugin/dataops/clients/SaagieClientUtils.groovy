package io.saagie.plugin.dataops.clients

import groovy.json.JsonSlurper

class SaagieClientUtils {
    static final EXPORTED_JOB_CONFIG_FILENAME = 'job.json'
    static final EXPORTED_JOB_PACKAGE_FOLDER_NAME = 'package'
    static final EXPORTED_PIPELINE_CONFIG_FILENAME = 'pipeline.json'
    static final EXPORTED_VARIABLE_CONFIG_FILENAME = 'variable.json'
    static final EXPORTED_APP_CONFIG_FILENAME = 'app.json'

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
        if (jobsFolder.exists()) {
            jobsFolder.eachFile { jobFolder ->
                String jobId = jobFolder.name
                String jobFolderPath = jobFolder.absolutePath

                extractedConfig.jobs[jobId] = [
                    configOverride: null,
                    package       : null
                ]

                def jsonParser = new JsonSlurper()
                File jobConfigFile = new File("${jobFolderPath}/${EXPORTED_JOB_CONFIG_FILENAME}")
                extractedConfig.jobs[jobId].configOverride = jsonParser.parse(jobConfigFile)

                if (extractedConfig.jobs[jobId].configOverride.versions) {
                    extractedConfig.jobs[jobId].configOverride.versions.collect {
                        def versionPackageName = new File("${jobFolderPath}/${EXPORTED_JOB_PACKAGE_FOLDER_NAME}/${it.number}").listFiles()
                        def versionPackageFileHead = null
                        if (versionPackageName && versionPackageName.head()) {
                            versionPackageFileHead = versionPackageName.head()
                        }
                        it.packageInfo = [
                            name: versionPackageFileHead
                        ]
                    }
                }

                def packageFile = new File("${jobFolderPath}/${EXPORTED_JOB_PACKAGE_FOLDER_NAME}").listFiles()
                def packageFileHead = null
                if (packageFile && packageFile.head()) {
                    packageFileHead = packageFile.find {
                        if (!it.isDirectory()) {
                            return true
                        }
                        return false
                    }
                }
                extractedConfig.jobs[jobId].package = packageFileHead
            }

            return extractedConfig
        }

        return null
    }

    def static extractPipelineConfigAndPackageFromExportedPipeline(File exportedZipFolder) {
        Map extractedConfig = [
            pipelines: [:],
        ]

        File pipelinesFolder = new File("${exportedZipFolder.absolutePath}/Pipeline")
        if (pipelinesFolder.exists()) {
            pipelinesFolder.eachFile { pipelineFolder ->
                String pipelineId = pipelineFolder.name
                String pipelineFolderPath = pipelineFolder.absolutePath

                extractedConfig.pipelines[pipelineId] = [
                    configOverride: null
                ]

                def jsonParser = new JsonSlurper()
                File pipelineConfigFile = new File("${pipelineFolderPath}/${EXPORTED_PIPELINE_CONFIG_FILENAME}")
                extractedConfig.pipelines[pipelineId].configOverride = jsonParser.parse(pipelineConfigFile)
            }

            return extractedConfig
        }
        return null
    }

    def static extractVariableConfigAndPackageFromExportedVariable(File exportedZipFolder) {
        Map extractedConfig = [
            variables: [:],
        ]

        File variablesFolder = new File("${exportedZipFolder.absolutePath}/Variable")
        if (variablesFolder.exists()) {
            variablesFolder.eachFile { variableFolder ->
                String variableId = variableFolder.name
                String variableFolderPath = variableFolder.absolutePath

                extractedConfig.variables[variableId] = [
                    configOverride: null
                ]

                def jsonParser = new JsonSlurper()
                File variableConfigFile = new File("${variableFolderPath}/${EXPORTED_VARIABLE_CONFIG_FILENAME}")
                extractedConfig.variables[variableId].configOverride = jsonParser.parse(variableConfigFile)
            }

            return extractedConfig
        }
        return null
    }


    def static extractAppConfigAndPackageFromExportedApp(File exportedAppFolder) {
        Map extractedConfig = [
            apps: [:],
        ]

        File appsFolder = new File("${exportedAppFolder.absolutePath}/App")
        if (appsFolder.exists()) {
            appsFolder.eachFile { appFolder ->
                String appId = appFolder.name
                String appFolderPath = appFolder.absolutePath

                extractedConfig.apps[appId] = [
                    configOverride: null
                ]

                def jsonParser = new JsonSlurper()
                File appConfigFile = new File("${appFolderPath}/${EXPORTED_APP_CONFIG_FILENAME}");
                extractedConfig.apps[appId].configOverride = jsonParser.parse(appConfigFile)
            }

            return extractedConfig
        }

        return null
    }



}
