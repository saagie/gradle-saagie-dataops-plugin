package io.saagie.plugin.dataops.tasks.service

import groovy.json.JsonSlurper
import io.saagie.plugin.dataops.utils.SaagieUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@Singleton
class TechnologyService {
    def technologies
    Map technologiesVersions = [:]
    OkHttpClient client
    def technologiesRequest
    def technologiesVersionsRequest
    JsonSlurper slurper
    static final Logger logger = Logging.getLogger(TechnologyService.class)
    def isInitialised = false
    def appTechnologyList
    def appTechnologiesRequest

    def init(
        client,
        technologiesRequest,
        technologiesVersionsRequest,
        slurper,
        allTechnologiesForAppRequest
    ) {
        this.client = client
        this.slurper = slurper
        this.technologiesRequest = technologiesRequest
        this.technologiesVersionsRequest = technologiesVersionsRequest
        this.appTechnologiesRequest = allTechnologiesForAppRequest
        this.isInitialised = true
    }

    def getTechnologies() {
        checkReady()
        Request technologiesCall = technologiesRequest()

        if (!technologies) {

            client.newCall(technologiesCall).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                logger.debug("getTechnologies response $responseBody")
                def parsedTechnologiesData = slurper.parseText(responseBody)
                if (!parsedTechnologiesData.data || !parsedTechnologiesData.data.technologies) {
                    throwAndLogError("Something went wrong when getting technologies")
                }
                technologies = parsedTechnologiesData.data.technologies
            }
        }
        return technologies
    }

    def getTechnologyVersions(String technologyId) {
        checkReady()
        if (!technologiesVersions || !technologiesVersions[technologyId]) {

            Request technologiesVersionRequestCall = technologiesVersionsRequest(technologyId)

            client.newCall(technologiesVersionRequestCall).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                logger.debug("getTechnologyVersions response $responseBody")
                def parsedTechnologyVersionsData = slurper.parseText(responseBody)
                if (!parsedTechnologyVersionsData.data || !parsedTechnologyVersionsData.data.technologiesVersions) {
                    return null
                }
                technologiesVersions[technologyId] = parsedTechnologyVersionsData.data.technologiesVersions
            }
        }
        return technologiesVersions[technologyId]
    }

    def checkReady() {
        if (!client || !technologiesRequest || !technologiesVersionsRequest) {
            throwAndLogError("You need to initialize the singleton TechnologyService with init(...)")
        }
    }

    def checkTechnologiesReady() {
        if (!technologies) {
            throwAndLogError("You need to initialize the technologies list")
        }
    }

    def checkTechnologyVersionsReady() {
        if (!technologiesVersions) {
            throwAndLogError("You need to initialize the technology versions list")
        }
    }

    static throwAndLogError(message) {
        logger.error(message)
        throw new GradleException(message)
    }

    static handleErrors(Response response) {
        SaagieUtils.handleErrorClosure(logger, response)
    }

    def getV2TechnologyByName(String name) {
        checkReady()
        if (name.equals('java-scala') || name.equals('java')) {
            return findTechnologyByName('Java/Scala')
        }

        if (name.equals('docker')) {
            return findTechnologyByName('Generic')
        }

        findTechnologyByName(name)
    }

    def findTechnologyByName(String name) {
        checkReady()
        if (!technologies) {
            getTechnologies()
        }
        checkTechnologiesReady()
        technologies.find {
            it.label.toUpperCase().equals(name.toUpperCase())
        }
    }

    def getMostRelevantTechnologyVersion(String technologyId, String versionV1, Map extraTechnologyV1, resultTechnologiesVersions) {

        if (!technologiesVersions) {
            getTechnologiesVersions()
        }

        checkTechnologyVersionsReady()
        def versionV2 = null

        if (!versionV1) {

            if (resultTechnologiesVersions != null && resultTechnologiesVersions.first() && resultTechnologiesVersions.first().technologyLabel == "Generic") {

                versionV2 = resultTechnologiesVersions.first()
                return [version2: versionV2, extraTechnology: null]

            } else {
                def convertedIntegerList = technologiesVersions[technologyId].collect { version ->
                    return convertToNumber(version.versionLabel)
                }
                def index = convertedIntegerList.findIndexOf {
                    it == convertedIntegerList.max()
                }
                if (index || index == 0) {
                    versionV2 = technologiesVersions[technologyId][index]
                }
            }

        }

        if (versionV1) {
            def upperCaseVersion2 = versionV1.toUpperCase()
            versionV2 = getMostReleventTechnologyVersion(technologyId, 'versionLabel', upperCaseVersion2)
        }

        if (!versionV2 && technologiesVersions[technologyId].size() > 0) {
            versionV2 = technologiesVersions[technologyId][0]
        }


        if (!versionV2) {
            return [null, null]
        }
        def extraTechnologyV2 = null

        if (versionV2.secondaryTechnologies && extraTechnologyV1 && extraTechnologyV1.version && extraTechnologyV1.language) {
            extraTechnologyV2 = getExtraTechnologiesLabelAndNumber(versionV2.secondaryTechnologies, extraTechnologyV1.language, extraTechnologyV1.version)
        }

        return [version2: versionV2, extraTechnology: extraTechnologyV2]
    }

    def getExtraTechnologiesLabelAndNumber(extraTechnologiesListV2, extraVersionLabelV1, extraVersionNumberV1) {

        def extraTechnologyV2 = getMostReleventExtraTechologyVersion(extraTechnologiesListV2, extraVersionLabelV1)
        if (!extraTechnologyV2 || !extraTechnologyV2["versions"]) {
            throwAndLogError("Cant't map extra technologies versions")
        }
        def numberVersionExtraTechnologyV2 = getMostReleventV2VersionByV1Version(extraTechnologyV2["versions"], null, extraVersionNumberV1)
        if (!numberVersionExtraTechnologyV2) {
            throwAndLogError("Couldn't get number version for extra technology")
        }
        return [language: extraTechnologyV2.label, version: numberVersionExtraTechnologyV2]
    }

    def getMostReleventExtraTechologyVersion(listTechnologyExtraV2, upperCaseVersionV1) {
        return getMostReleventV2VersionByV1Version(listTechnologyExtraV2, 'label', upperCaseVersionV1)
    }

    def getMostReleventTechnologyVersion(technologyId, accessVersionProperty, upperCaseVersionV1) {
        return getMostReleventV2VersionByV1Version(technologiesVersions[technologyId], accessVersionProperty, upperCaseVersionV1)
    }

    def getMostReleventV2VersionByV1Version(listVersionElements, accessVersionProperty, upperCaseVersionV1) {
        def stringVersionV1 = upperCaseVersionV1.toString().toUpperCase() as String
        if (!SaagieUtils.isCollectionOrArray(listVersionElements)) {
            return null
        }

        def versionV2 = listVersionElements.find {
            accessVersionProperty ? it[accessVersionProperty].toUpperCase().equals(stringVersionV1) : it.toUpperCase().equals(stringVersionV1)
        }

        if (!versionV2) {
            versionV2 = listVersionElements.find {
                accessVersionProperty ? it[accessVersionProperty].toUpperCase().startsWith(stringVersionV1) : it.toUpperCase().startsWith(stringVersionV1)
            }
        }

        if (!versionV2) {
            if (stringVersionV1.toString().lastIndexOf(".") > 0) {
                def reducedUpperCaseVersionV1 = stringVersionV1.substring(0, stringVersionV1.lastIndexOf("."))
                if (reducedUpperCaseVersionV1 != stringVersionV1) {
                    return getMostReleventV2VersionByV1Version(listVersionElements, accessVersionProperty, reducedUpperCaseVersionV1)
                } else if (listVersionElements.size() > 0) {
                    return listVersionElements[0]
                }
            }
            return null
        }
        return versionV2
    }

    def convertToNumber(String technologyLabel) {
        return technologyLabel.findAll(/\d+/)*.toInteger().join().toInteger()
    }

    def getAppTechnologies() {
        checkReady()
        Request appTechnologiesCall = appTechnologiesRequest()

        if (!appTechnologyList) {

            client.newCall(appTechnologiesCall).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                logger.debug("getAppTechnologies response $responseBody")
                def parsedTechnologiesData = slurper.parseText(responseBody)
                if (!parsedTechnologiesData.data || !parsedTechnologiesData.data.repositories) {
                    throwAndLogError("Something went wrong when getting app technologies")
                }
                def repo = parsedTechnologiesData.data.repositories
                appTechnologyList = this.getTechnologiesFromRepositories(repo)
            }
        }
        return appTechnologyList
    }

    def checkTechnologyIdExistInAppTechnologyList(String technologyId) {
        def tech = this.appTechnologyList.find{it.label?.equals(technologyId)}
        return tech
    }

    def getTechnologiesFromRepositories(repositories){
       def technologyList = []
        repositories.forEach{ repositorie ->
            def list = repositorie.technologies as List
            technologyList=(technologyList<<list).flatten()
        }
        return technologyList
    }
}
