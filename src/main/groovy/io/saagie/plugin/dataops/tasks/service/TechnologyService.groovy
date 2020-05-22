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
        if(name.equals('java-scala')){
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

    def getMostRelevantTechnologyVersion(String technologyId,String versionV1) {
        if(!technologiesVersions) {
            getTechnologiesVersions()
        }
        checkTechnologyVersionsReady()
        if(!versionV1) {
            def convertedIntegerList =  technologiesVersions[technologyId].collect { version ->
                return convertToNumber(version.versionLabel)
            }
            def index = convertedIntegerList.findIndexOf {
                it == convertedIntegerList.max()
            }
            if(!index && index != 0) {
               return null
            }
            return technologiesVersions[technologyId][index]
        }
        def upperCaseVersion2 = versionV1.toUpperCase()
        return getVersionUntilFind(technologyId, upperCaseVersion2)
        if(!versionV2){
            return null
        }

    }

    def getVersionUntilFind(technologyId, upperCaseVersion2){
        def stringVersion2 = upperCaseVersion2.toString()
        def versionV2 = technologiesVersions[technologyId].find { it.versionLabel.toUpperCase().equals(stringVersion2) }
        if(!versionV2) {
            versionV2 =  technologiesVersions[technologyId].find {
                it.versionLabel.toUpperCase().startsWith(stringVersion2)
            }
        }
        if(!versionV2){
            if(stringVersion2.toString().lastIndexOf(".") > 0) {
                def reducedUpperCaseVersion2 = stringVersion2.substring(0, stringVersion2.lastIndexOf("."))
                if(reducedUpperCaseVersion2  != stringVersion2) {
                    return getVersionUntilFind(technologyId, reducedUpperCaseVersion2)
                } else if (technologiesVersions[technologyId].size()  > 0) {
                    return technologiesVersions[technologyId][0]
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
