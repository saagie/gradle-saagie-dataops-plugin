package io.saagie.plugin.tasks.utils

import io.saagie.plugin.dataops.utils.SaagieUtils
import spock.lang.Specification
import spock.lang.Title

@Title("Utils classes tests")
class CronConvertionTests extends Specification  {
    def "Cron convertion : Schedule v1 convertion must produce valide cron"() {
        given:
            def listDateFromV1 = [
                "R10/2020-05-12T14:47:15.525Z/PT2H12M",
                "R0/2019-12-06T08:59:29.336Z/P0Y0M1DT0H0M0S",
                "R0/2018-01-25T17:17:54.008Z/P0Y0M1DT0H0M0S",
                "R0/2020-01-29T16:14:44.866Z/P0Y0M1DT0H0M0S",
                "R/2020-05-12T14:45:55.899Z/P1D",
                "R/2019-04-05T09:37:46.273Z/P1M"
            ]
            def listExpectedValues = [
                "",
                "",
                "",
                "",
                "",
                "",
            ]
        def results = []
        when:
            results = listDateFromV1.collect {element ->
                return SaagieUtils.convertScheduleV1ToCron(element)
            }
            def commons = listDateFromV1.intersect(results)
            def difference = listDateFromV1.plus(results)
            difference.removeAll(commons)
        then:
        assert difference.isEmpty()
    }
}
