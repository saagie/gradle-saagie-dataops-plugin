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
        result.output.contains 'projectsCreateJob - Create a brand new job in a project'
        result.output.contains 'projectsCreatePipeline - Create a pipeline'
        result.output.contains 'projectsGetJobInstanceStatus - Get the status of a job instance'
        result.output.contains 'projectsGetPipelineInstanceStatus - Get the status of a pipeline instance'
        result.output.contains 'projectsList - List all projects on the environment'
        result.output.contains 'projectsListJobs - List all jobs of a project'
        result.output.contains 'projectsListTechnologies - List all technologies of a project'
        result.output.contains 'projectsRunJob - Run an existing job'
        result.output.contains 'projectsRunPipeline - Run a pipeline'
        result.output.contains 'projectsStopPipelineInstance - Stop a pipeline instance'
        result.output.contains 'projectsUpgradePipeline - Upgrade a pipeline'
        result.output.contains 'projectsUpgradeJob - Upgrade a existing job in a project'
        result.output.contains 'platformList - List available platforms'
        result.output.contains 'projectsListPipelines - List all pipelines of a project'
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
