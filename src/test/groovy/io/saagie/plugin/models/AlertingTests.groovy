package io.saagie.plugin.models

import io.saagie.plugin.dataops.models.Alerting
import spock.lang.Specification
import spock.lang.Title

@Title('Alerting model tests')
class AlertingTests extends Specification {

    def "given a login list, the toMap method should return a list of logins"() {
        given:
        def logins = ['login.test']
        def statusList = ['FAILED']
        Alerting alerting = new Alerting(
                logins: logins,
                statusList: statusList
        )

        when:
        Map mappedResult = alerting.toMap()

        then:
        mappedResult.emails == null
        mappedResult.logins == logins
        mappedResult.statusList == statusList
    }

    def "given an email list, the toMap method should return a list of emails"() {
        given:
        def emails = ['email@mail.com']
        def statusList = ['FAILED']
        Alerting alerting = new Alerting(
                emails: emails,
                statusList: statusList
        )

        when:
        Map mappedResult = alerting.toMap()

        then:
        notThrown(Exception)
        mappedResult.logins == null
        mappedResult.emails == emails
        mappedResult.statusList == statusList
    }

    def "given an email list and a logins list, the toMap method should use logins"() {
        given:
        def emails = ['email@mail.com']
        def logins = ['login.test']
        def statusList = ['FAILED']
        Alerting alerting = new Alerting(
                emails: emails,
                logins: logins,
                statusList: statusList
        )

        when:
        Map mappedResult = alerting.toMap()

        then:
        notThrown(Exception)
        mappedResult.emails == null
        mappedResult.logins == logins
        mappedResult.statusList == statusList
    }
}
