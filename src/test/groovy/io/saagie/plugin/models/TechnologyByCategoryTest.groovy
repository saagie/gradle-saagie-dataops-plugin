package io.saagie.plugin.models

import io.saagie.plugin.dataops.models.TechnologyByCategory
import spock.lang.Specification
import spock.lang.Title

@Title("Tests on TechnologyByCategory model")
class TechnologyByCategoryTest extends Specification {

    def "the model should have default values"() {
        given:
        TechnologyByCategory technologyByCategory = new TechnologyByCategory()

        expect:
        !technologyByCategory.exists()
        !technologyByCategory.toMap()
    }

    def "the model should return a map with only required provided data"() {
        given:
        TechnologyByCategory technologyByCategory = new TechnologyByCategory()
        technologyByCategory.category = "job-category"

        expect:
        technologyByCategory.exists()
    }

    def "the model should return a correct map when the toMap method is called"() {
        given:
        TechnologyByCategory technologyByCategory = new TechnologyByCategory()
        technologyByCategory.category = "job-category"

        when:
        Map result = technologyByCategory.toMap()

        then:
        technologyByCategory.exists()
        result == [
            jobCategory : "job-category",
            technologies: []
        ]
    }

    def "the model should return a correct map when the toMap method is called and technologies are provided"() {
        given:
        TechnologyByCategory technologyByCategory = new TechnologyByCategory()
        technologyByCategory.category = "job-category"
        technologyByCategory.technologyid = [
            "techno1", "techno2"
        ]

        when:
        Map result = technologyByCategory.toMap()

        then:
        technologyByCategory.exists()
        result.size() == 2
        result.containsKey("technologies")
        result.containsKey("jobCategory")
        result.jobCategory == "job-category"
        result.technologies == [[id: "techno1"], [id: "techno2"]]
    }

}
