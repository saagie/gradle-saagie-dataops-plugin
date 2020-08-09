package io.saagie.plugin.tasks.artifacts

import io.saagie.plugin.DataOpsGradleTaskSpecification
import io.saagie.plugin.dataops.DataOpsModule
import org.gradle.testkit.runner.BuildResult
import spock.lang.Shared
import spock.lang.Title

@Title('ArtifactsImportV2EnvVarTaskTests task tests')
class ArtifactsImportV2EnvVarTaskTests  extends DataOpsGradleTaskSpecification{
	@Shared String taskName = DataOpsModule.PROJECTS_IMPORT_JOB
	
	def "the task should export environments variables of scope global"() {
		given:
		
		File tempJobDirectory = File.createTempDir("project", ".tmp")
		
		enqueueRequest("{\"data\":{\"globalEnvironmentVariables\":[{\"id\":\"global-1-id\",\"name\":\"testUX2\",\"scope\":\"GLOBAL\",\"value\":\"lala1\",\"description\":\"lala1\",\"isPassword\":false},{\"id\":\"global-2-id\",\"name\":\"globalvar\",\"scope\":\"GLOBAL\",\"value\":\"GlobalEnvVAr\",\"description\":\"global variable\",\"isPassword\":false},{\"id\":\"global-3-id\",\"name\":\"bbbb\",\"scope\":\"GLOBAL\",\"value\":\"bbbbb\",\"description\":\"\",\"isPassword\":false},{\"id\":\"global-4-id\",\"name\":\"WEB_URL\",\"scope\":\"GLOBAL\",\"value\":null,\"description\":\"\",\"isPassword\":true},{\"id\":\"global-5-id\",\"name\":\"PORT_SEARCH\",\"scope\":\"GLOBAL\",\"value\":\"1000\",\"description\":\"\",\"isPassword\":false},{\"id\":\"global-6-id\",\"name\":\"IP_PALAMA\",\"scope\":\"GLOBAL\",\"value\":\"192.168.1.1\",\"description\":\"\",\"isPassword\":false},{\"id\":\"global-7-id\",\"name\":\"IP_SEARCH\",\"scope\":\"GLOBAL\",\"value\":\"192.168.1.1\",\"description\":\"\",\"isPassword\":false}]}}")
		
		buildFile << """
           saagie {
                server {
                   url = 'http://localhost:9000/'
                    login = 'user'
                    password = 'password'
                    environment = 1
                    useLegacy = false
                }
				
				project {
                    id = 'project-id'
				}

                exportArtifacts {
                  export_file = "${tempJobDirectory.getAbsolutePath()}/testvariable1.zip"
                  overwrite=true
                }
           }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		def computedValue = """{"status":"success","exportfile":"${tempJobDirectory.getAbsolutePath()}/testvariable1.zip"}"""
		
		then:
		notThrown(Exception)
		assert new File("${tempJobDirectory.getAbsolutePath()}/testvariable1.zip").exists()
		result.output.contains(computedValue)
	}
}
