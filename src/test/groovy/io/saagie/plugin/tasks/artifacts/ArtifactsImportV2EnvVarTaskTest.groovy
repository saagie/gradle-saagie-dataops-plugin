package io.saagie.plugin.tasks.artifacts

import io.saagie.plugin.DataOpsGradleTaskSpecification
import io.saagie.plugin.dataops.DataOpsModule
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared

import static org.gradle.testkit.runner.TaskOutcome.FAILED

class ArtifactsImportV2EnvVarTaskTest extends DataOpsGradleTaskSpecification {
	@Shared String taskName = DataOpsModule.PROJECTS_IMPORT_JOB
	@Shared ClassLoader classLoader = getClass().getClassLoader()
	@Shared String exportVariableWithoutJobAndPipeline = './exportVariableWithoutJobAndPipeline.zip'
	
	def "the task should import environment variable on the platform"() {
		given:
		URL resource = classLoader.getResource(exportVariableWithoutJobAndPipeline)
		File exportedConfig = new File(resource.getFile())
		enqueueRequest('{"data":{"projectEnvironmentVariables":[{"id":"variable-id-1","scope":"GLOBAL","name":"testUX2","value":"lala1","description":"lala1","isPassword":false,"overriddenValues":[]},{"id":"variable-id-2","scope":"GLOBAL","name":"sjones3423","value":null,"description":"description","isPassword":true,"overriddenValues":[]},{"id":"variable-id-3","scope":"GLOBAL","name":"sjones","value":null,"description":"description","isPassword":true,"overriddenValues":[]},{"id":"variable-id-4","scope":"PROJECT","name":"globalvar","value":"pro","description":"pro","isPassword":false,"overriddenValues":[{"id":"variable-id-5","scope":"GLOBAL","value":"GlobalEnvVAr","description":"global variable","isPassword":false}]}]}}')
		enqueueRequest('{"data":{"saveEnvironmentVariable":{"id":"variable-1","name":"GLOBAL_AMINE","__typename":"EnvironmentVariable"}}}')
		enqueueRequest('{"data":{"saveEnvironmentVariable":{"id":"variable-2","name":"globalvar","__typename":"EnvironmentVariable"}}}')
		buildFile << """
            saagie {
                server {
                    url = '${mockServerUrl}'
                    login = 'login'
                    password = 'password'
                    environment = 1
                }

                project {
                    id = 'project-id'
                }
                
                importArtifacts {
                    import_file = '${exportedConfig.absolutePath}'
                }
            }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		then:
		notThrown(Exception)
		result.output.contains('{status=success, job=[], pipeline=[], variable=[{id=variable-1, name=GLOBAL_AMINE}, {id=variable-2, name=globalvar}]}')
	}
}
