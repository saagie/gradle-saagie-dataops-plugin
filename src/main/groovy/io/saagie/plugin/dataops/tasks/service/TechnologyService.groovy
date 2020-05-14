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
    Map technologiesVersions
    OkHttpClient client
    Request technologiesRequest
    Request technologiesVersionsRequest
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
        if(!technologiesData) {
            client.newCall(technologiesRequest).execute().withCloseable { response ->
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
        if(!technologiesDataVersions[technologyId]) {
            client.newCall(technologiesVersionsRequest(technologyId)).execute().withCloseable { response ->
                handleErrors(response)
                String responseBody = response.body().string()
                def parsedTechnologyVersionsData = slurper.parseText(responseBody)
                if(!parsedTechnologyVersionsData.data || !parsedTechnologyVersionsData.data.technologiesVersions) {
                    throwAndLogError("Something went wrong when getting technologies versions")
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

    // TODO Add find by name method
    // TODO Add find latest version of technology
    // TODO add Capitelize string way

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
            def convertedIntegerList =  technologies[technologyId].collect { version ->
                return convertToNumber(version.technologyLabel)
            }
            def index = convertedIntegerList.findIndexOf {it.value == convertedIntegerList.max()}
            if(!index) {
                throwAndLogError("Couldn t find index for the lastest technology version")
            }
            return technologies[technologyId][index].technologyLabel
        }
        def upperCaseVersion2 = versionV1.toUpperCase()
        def versionV2 = technologies[technologyId].find { it.technologyLabel.toUpperCase().equals(upperCaseVersion2) }
        if(!versionV2) {
            versionV2 =  technologies[technologyId].find { it.technologyLabel.toUpperCase().startsWith(upperCaseVersion2)}
        }
        if(versionV2){
            return versionV2
        }
        throwAndLogError('Couldn t get the version V2 from version V1')
    }

    def convertToNumber(String technologyLabel) {
        return technologyLabel.findAll { Character.isDigit(it) }
    }
}
