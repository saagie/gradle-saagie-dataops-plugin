package io.saagie.plugin.dataops.clients

import io.saagie.plugin.dataops.DataOpsExtension
import org.slf4j.LoggerFactory
import spock.lang.*


class SaagieClientTest extends Specification {

    SaagieClient saagieClient
    def dataopsConfig = new DataOpsExtension();
    def log = LoggerFactory.getLogger('saagie-client-test')

    def setup() {
        dataopsConfig.server {
            url = 'https://saagie-beta.prod.saagie.io'
            login = 'renan.decamps'
            password = 'McVities$76%!1994'
            environment = 6
        };
        saagieClient = new SaagieClient(dataopsConfig)
    }

    def "configuration attribute should be equivalent as the one provided"() {
        expect:
        saagieClient.configuration.server.url == 'https://saagie-beta.prod.saagie.io'
        saagieClient.configuration.server.login == 'renan.decamps'
        saagieClient.configuration.server.password == 'McVities$76%!1994'
        saagieClient.configuration.server.environment == '6'
    }

    def "getProjects request should be a array of projects"() {
        given:
        def projects = saagieClient.getProjects()

        expect:
        projects instanceof List
        projects[0].id == '8321e13c-892a-4481-8552-5be4b6cc5df4'
    }
}
