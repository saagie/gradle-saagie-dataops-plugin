package io.saagie.plugin.tasks.artifacts

import io.saagie.plugin.DataOpsGradleTaskSpecification
import io.saagie.plugin.dataops.DataOpsModule
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared

import static org.gradle.testkit.runner.TaskOutcome.FAILED

class ArtifactsImportV1Test extends DataOpsGradleTaskSpecification {
	@Shared String taskName = DataOpsModule.PROJECTS_IMPORT_JOB
	@Shared ClassLoader classLoader = getClass().getClassLoader()
	@Shared String exportedPipelineWithoutJobForV1 = './exportedPipelineWithoutJobForV1.zip'
	
	def "the task should fail pipeline import if jobs from pipeline doesn't exist on platform"() {
		given:
		URL resource = classLoader.getResource(exportedPipelineWithoutJobForV1)
		File exportedConfig = new File(resource.getFile())
		enqueueRequest('{"data":{"jobs":[{"id":"job-1","name":"job name not contained in the pipeline version"}, {"id":"job-2","name":"job name name contained in the pipeline version"}]}}')
		
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
		BuildResult result = gradle(taskName, "-d")
		
		then:
		UnexpectedBuildFailure e = thrown()
		result == null
		e.message.contains("Missing job names not found on the target platform => : [Missing job name]")
		e.getBuildResult().task(":${taskName}").outcome == FAILED}
}
