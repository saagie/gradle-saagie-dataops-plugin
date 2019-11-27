package io.saagie.plugin.models

import io.saagie.plugin.dataops.models.Project
import io.saagie.plugin.dataops.models.SecurityGroup
import io.saagie.plugin.dataops.models.TechnologyByCategory
import spock.lang.Specification
import spock.lang.Title

@Title("Tests project model")
class ProjectTest extends Specification {

    def 'if no id is specified, the returned model should be the one use to create a project'() {
        given:
        Project project = new Project()
        project.name = 'my-project'

        when:
        Map generatedMap = project.toMap()

        then:
        generatedMap != null
        generatedMap.size() == 4
        generatedMap.containsKey('name')
        generatedMap.name == 'my-project'

        generatedMap.containsKey('description')
        generatedMap.description == null

        generatedMap.containsKey('technologiesByCategory')
        generatedMap.technologiesByCategory == null

        generatedMap.containsKey('authorizedGroups')
        generatedMap.authorizedGroups == []
    }

    def 'if no id is specified, the returned model should be have the correct format when all params are provided'() {
        given:
        Project project = new Project(
            name: 'my-project',
            description: 'my-description',
            authorizedGroups: [
                new SecurityGroup(),
                new SecurityGroup(name: 'security-group'),
                new SecurityGroup(name: 'security-group-2', projectRole: 'ROLE_PROJECT_VIEWER'),
            ],
            technologyByCategory: [
                new TechnologyByCategory(),
                new TechnologyByCategory(jobCategory: 'job-category'),
                new TechnologyByCategory(jobCategory: 'job-category-2', technologyid: []),
                new TechnologyByCategory(jobCategory: 'job-category-3', technologyid: ['1']),
            ]
        )

        when:
        Map generatedMap = project.toMap()

        then:
        generatedMap != null
        generatedMap.size() == 4
        generatedMap.containsKey('name')
        generatedMap.name == 'my-project'

        generatedMap.containsKey('description')
        generatedMap.description == 'my-description'

        generatedMap.containsKey('technologiesByCategory')
        generatedMap.technologiesByCategory.size() == 4
        generatedMap.technologiesByCategory[0] == null
        generatedMap.technologiesByCategory[1] == [jobCategory: 'job-category', technologies: null]
        generatedMap.technologiesByCategory[2] == [jobCategory: 'job-category-2', technologies: null]
        generatedMap.technologiesByCategory[3] == [jobCategory: 'job-category-3', technologies: [[id:'1']]]

        generatedMap.containsKey('authorizedGroups')
        generatedMap.authorizedGroups.size == 3
        generatedMap.authorizedGroups[0] == null
        generatedMap.authorizedGroups[1] == [name: 'security-group', projectRole: null]
        generatedMap.authorizedGroups[2] == [name: 'security-group-2', projectRole: 'ROLE_PROJECT_VIEWER']
    }

}
