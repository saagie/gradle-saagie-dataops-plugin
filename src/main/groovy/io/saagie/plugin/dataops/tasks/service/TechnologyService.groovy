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

    def init(
        client,
        technologiesRequest,
        technologiesVersionsRequest,
        slurper
    ) {
        this.client = client
        this.slurper = slurper
        this.technologiesRequest = technologiesRequest
        this.technologiesVersionsRequest = technologiesVersionsRequest
    }

    def getTechnologies() {
        checkReady()
        Request technologiesCall = technologiesRequest()
        if(!technologies) {
            client.newCall(technologiesCall).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedTechnologiesData = slurper.parseText(responseBody)
                if(!parsedTechnologiesData.data || !parsedTechnologiesData.data.technologies) {
                    throwAndLogError("Something went wrong when getting technologies")
                }
                technologies = parsedTechnologiesData.data.technologies
            }
        }
        return technologies
    }

    def getTechnologyVersions(String technologyId) {
        checkReady()
        if(!technologiesVersions || !technologiesVersions[technologyId]) {
            Request technologiesVersionRequestCall = technologiesVersionsRequest(technologyId)
            client.newCall(technologiesVersionRequestCall).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedTechnologyVersionsData = slurper.parseText(responseBody)
                if(!parsedTechnologyVersionsData.data || !parsedTechnologyVersionsData.data.technologiesVersions) {
                    return null
                }
                technologiesVersions[technologyId] = parsedTechnologyVersionsData.data.technologiesVersions
            }
        }
        return technologiesVersions[technologyId]
    }

    def checkReady() {
        if(!client || !technologiesRequest || !technologiesVersionsRequest) {
            throwAndLogError("You need to initialize the singleton TechnologyService with init(...)")
        }
    }

    def checkTechnologiesReady() {
        if(!technologies) {
            throwAndLogError("You need to initialize the technologies list")
        }
    }

    def checkTechnologyVersionsReady() {
        if(!technologiesVersions) {
            throwAndLogError("You need to initialize the technology versions list")
        }
    }

    static throwAndLogError(message){
        logger.error(message)
        throw new GradleException(message)
    }

    static handleErrors(Response response) {
        SaagieUtils.handleErrorClosure(logger, response)
    }

    def getV2TechnologyByV1Name(String name) {
        checkReady()
        if(name.equals('java-scala') || name.equals('java')){
            return findTechnologyByName('Java/Scala')
        }
        findTechnologyByName(name)
    }

    def findTechnologyByName(String name) {
        checkReady()
        if(!technologies) {
            getTechnologies()
        }
        checkTechnologiesReady()
        technologies.find { it.label.toUpperCase().equals(name.toUpperCase()) }
    }

    def getMostRelevantTechnologyVersion(String technologyId, String versionV1, Map extraTechnologyV1) {
        if(!technologiesVersions) {
            getTechnologiesVersions()
        }
        checkTechnologyVersionsReady()

        def versionV2 = null

        if(!versionV1) {
            def convertedIntegerList =  technologiesVersions[technologyId].collect { version ->
                return convertToNumber(version.versionLabel)
            }
            def index = convertedIntegerList.findIndexOf {
                it == convertedIntegerList.max()
            }
            if(index || index == 0) {
                versionV2 = technologiesVersions[technologyId][index]
            }
        }

        if(versionV1) {
            def upperCaseVersion2 = versionV1.toUpperCase()
            versionV2= getTechnologyVersionUntilFind(technologyId, 'versionLabel', upperCaseVersion2)
        }

        if(!versionV2 && technologiesVersions[technologyId].size() > 0) {
            versionV2 = technologiesVersions[technologyId][0]
        }


        if(!versionV2){
            return [null, null]
        }
        def extraTechnologyV2 = null

        if(versionV2.secondaryTechnologies && extraTechnologyV1 && extraTechnologyV1.version && extraTechnologyV1.language) {
            extraTechnologyV2 = getExtraTechnologiesLabelAndNumber(versionV2.secondaryTechnologies, extraTechnologyV1.language, extraTechnologyV1.version)
        }

        return [version2: versionV2, extraTechnology: extraTechnologyV2]
    }

    def getExtraTechnologiesLabelAndNumber(extraTechnologiesListV2, extraVersionLabelV1, extraVersionNumberV1) {

        def extraTechnologyV2 =  getExtraTechnologyVersionUntilFind(extraTechnologiesListV2, extraVersionLabelV1)
        if(!extraTechnologyV2 || !extraTechnologyV2["versions"]) {
            throwAndLogError("Cant't map extra technologies versions")
        }
        def numberVersionExtraTechnologyV2 = getUntilFind(extraTechnologyV2["versions"], null, extraVersionNumberV1)
        if(!numberVersionExtraTechnologyV2) {
            throwAndLogError("Couldn t get number version for extra technology")
        }
        return [language: extraTechnologyV2.label, version: numberVersionExtraTechnologyV2]
    }

    def getExtraTechnologyVersionUntilFind(listTechnologyExtraV2, upperCaseVersionV1) {
        return getUntilFind(listTechnologyExtraV2, 'label', upperCaseVersionV1)
    }

    def getTechnologyVersionUntilFind(technologyId, accessVersionProperty, upperCaseVersionV1){
        return getUntilFind(technologiesVersions[technologyId], accessVersionProperty, upperCaseVersionV1)
    }

    def getUntilFind(listVersionElements, accessVersionProperty, upperCaseVersionV1) {
        def stringVersionV1 = upperCaseVersionV1.toString().toUpperCase() as String
        if(!SaagieUtils.isCollectionOrArray(listVersionElements)) {
            return null
        }

        def versionV2 = listVersionElements.find {
            accessVersionProperty? it[accessVersionProperty].toUpperCase().equals(stringVersionV1) : it.toUpperCase().equals(stringVersionV1)
        }

        if(!versionV2) {
            versionV2 =  listVersionElements.find {
                accessVersionProperty ? it[accessVersionProperty].toUpperCase().startsWith(stringVersionV1) : it.toUpperCase().startsWith(stringVersionV1)
            }
        }

        if(!versionV2){
            if(stringVersionV1.toString().lastIndexOf(".") > 0) {
                def reducedUpperCaseVersionV1 = stringVersionV1.substring(0, stringVersionV1.lastIndexOf("."))
                if(reducedUpperCaseVersionV1  != stringVersionV1) {
                    return getUntilFind(listVersionElements, accessVersionProperty, reducedUpperCaseVersionV1)
                } else if (listVersionElements.size()  > 0) {
                    return listVersionElements[0]
                }
            }
            return null
        }
        return versionV2
    }

    def convertToNumber(String technologyLabel) {
        return technologyLabel.findAll( /\d+/ )*.toInteger().join().toInteger()
    }
}
