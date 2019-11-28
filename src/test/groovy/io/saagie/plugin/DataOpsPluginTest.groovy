package io.saagie.plugin

import org.gradle.testkit.runner.BuildResult
import spock.lang.*

@Title("Plugin integration test with gradle")
class DataOpsPluginTest extends DataOpsGradleTaskSpecification {

    def "gradle tasks should show all tasks under a Saagie group"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }
            }
        """

        when:
        BuildResult result = gradle ('tasks', '--all')

        then:
        notThrown(Exception)
        result.output.contains 'Saagie tasks'
        result.output.contains 'projectsCreateJob - create a brand new job in a project'
        result.output.contains 'projectsCreatePipeline - create a pipeline'
        result.output.contains 'projectsGetJobInstanceStatus - get the status of a job instance'
        result.output.contains 'projectsGetPipelineInstanceStatus - get the status of a pipeline instance'
        result.output.contains 'projectsList - list all projects on the environment'
        result.output.contains 'projectsListJobs - list all jobs of a project'
        result.output.contains 'projectsListTechnologies - list all technologies of a project'
        result.output.contains 'projectsRunJob - run an existing job'
        result.output.contains 'projectsRunPipeline - run a pipeline'
        result.output.contains 'projectsStopPipelineInstance - stop a pipeline instance'
        result.output.contains 'projectsUpdatePipeline - update a pipeline'
        result.output.contains 'projectsUpdateJob - update a existing job in a project'
        result.output.contains 'platformList - list available platforms'
        result.output.contains 'projectsListPipelines - list all pipelines of a project'
    }

    def "all requests must fail if the required params are not provided"() {
        given:
        buildFile << '''
            saagie {
                server {
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                }
            }
        '''

        when:
        def result = gradle ('platformList')

        then:
        Exception e = thrown()
        result == null
        e.message.contains('url cannot be empty')
        e.message.contains('Missing required params in plugin configuration, check that you have url, environment, login and password defined in your server object.')
    }
}
