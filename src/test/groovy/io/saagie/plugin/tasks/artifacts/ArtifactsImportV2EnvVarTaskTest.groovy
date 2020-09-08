package io.saagie.plugin.tasks.artifacts

import io.saagie.plugin.DataOpsGradleTaskSpecification
import io.saagie.plugin.dataops.DataOpsModule
import org.gradle.testkit.runner.BuildResult
import spock.lang.Shared

class ArtifactsImportV2EnvVarTaskTest extends DataOpsGradleTaskSpecification {
	@Shared
	String taskName = DataOpsModule.PROJECTS_IMPORT_ARTIFACTS_JOB
	@Shared
	ClassLoader classLoader = getClass().getClassLoader()
	@Shared
	String exportVariableWithoutJobAndPipeline = './exportVariableWithoutJobAndPipeline.zip'
	
	def "the task should import environment variable on the platform"() {
		given:
		URL resource = classLoader.getResource(exportVariableWithoutJobAndPipeline)
		File exportedConfig = new File(resource.getFile())
		enqueueRequest('{"data":{"saveEnvironmentVariable":{"id":"variable-1","name":"GLOBAL_AMINE","__typename":"EnvironmentVariable"}}}')
		enqueueRequest('{"data":{"saveEnvironmentVariable":{"id":"variable-2","name":"testUX2","__typename":"EnvironmentVariable"}}}')
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
		result.output.contains('{status=success, job=[], pipeline=[], variable=[{id=variable-1, name=GLOBAL_AMINE}, {id=variable-2, name=testUX2}]}')
	}
}
