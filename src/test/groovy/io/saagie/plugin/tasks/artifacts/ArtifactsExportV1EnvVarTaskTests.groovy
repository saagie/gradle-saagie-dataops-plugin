package io.saagie.plugin.tasks.artifacts

import io.saagie.plugin.DataOpsGradleTaskSpecification
import io.saagie.plugin.dataops.DataOpsModule
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('ArtifactsExportV1EnvVarTaskTests task tests')
class ArtifactsExportV1EnvVarTaskTests extends DataOpsGradleTaskSpecification {
	@Shared String taskName = DataOpsModule.PROJECTS_EXPORT_ARTIFACTS_V1
	
	def "the task should export v1 all environments variables when include_all_var is true  with project scope"() {
		given:
		
		File tempJobDirectory = File.createTempDir("project", ".tmp")
		
		enqueueRequest("[{\"id\":1,\"name\":\"MONGO_IP_TEST\",\"value\":\"192.168.1.1\",\"isPassword\":false,\"platformId\":1},{\"id\":2,\"name\":\"MONGO_PORT_TEST\",\"value\":\"77777\",\"isPassword\":false,\"platformId\":1}]")
		
		buildFile << """
           saagie {
                server {
                   url = 'http://localhost:9000/'
                    login = 'user'
                    password = 'password'
                    environment = 1
                    useLegacy = false
                }

                env {
                  scope = 'project'
                  include_all_var = true
                }

                exportArtifacts {
                  export_file = "${tempJobDirectory.getAbsolutePath()}/testVariableWithScopeProject.zip"
                  overwrite=true
                }
           }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		def computedValue = """{"status":"success","exportfile":"${tempJobDirectory.getAbsolutePath()}/testVariableWithScopeProject.zip"}"""
		
		then:
		notThrown(Exception)
		assert new File("${tempJobDirectory.getAbsolutePath()}/testVariableWithScopeProject.zip").exists()
		result.output.contains(computedValue)
	}
	
	def "the task should export v1 all environments variables when include_all_var is true with global scope"() {
		given:
		
		File tempJobDirectory = File.createTempDir("project", ".tmp")
		
		enqueueRequest("[{\"id\":1,\"name\":\"MONGO_IP_TEST\",\"value\":\"192.168.1.1\",\"isPassword\":false,\"platformId\":1},{\"id\":2,\"name\":\"MONGO_PORT_TEST\",\"value\":\"77777\",\"isPassword\":false,\"platformId\":1}]")
		
		buildFile << """
           saagie {
                server {
                   url = 'http://localhost:9000/'
                    login = 'user'
                    password = 'password'
                    environment = 1
                    useLegacy = false
                }
				
                env {
                  scope = 'global'
                  include_all_var = true
                }

                exportArtifacts {
                  export_file = "${tempJobDirectory.getAbsolutePath()}/testVariableWithScopeGlobal.zip"
                  overwrite=true
                }
           }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		def computedValue = """{"status":"success","exportfile":"${tempJobDirectory.getAbsolutePath()}/testVariableWithScopeGlobal.zip"}"""
		
		then:
		notThrown(Exception)
		assert new File("${tempJobDirectory.getAbsolutePath()}/testVariableWithScopeGlobal.zip").exists()
		result.output.contains(computedValue)
	}
	
	def "the task should export v1 only environments variables within name attribute when include_all_var is false"() {
		given:
		
		File tempJobDirectory = File.createTempDir("project", ".tmp")
		
		enqueueRequest("[{\"id\":1,\"name\":\"MONGO_IP_TEST\",\"value\":\"192.168.1.1\",\"isPassword\":false,\"platformId\":1},{\"id\":2,\"name\":\"MONGO_PORT_TEST\",\"value\":\"77777\",\"isPassword\":false,\"platformId\":1}]")
		
		buildFile << """
           saagie {
                server {
                   url = 'http://localhost:9000/'
                    login = 'user'
                    password = 'password'
                    environment = 1
                    useLegacy = false
                }
	
                env {
                   include_all_var = false
                   scope = 'project'
                   name=['MONGO_IP_TEST', 'MONGO_PORT_TEST']
                }

                exportArtifacts {
                  export_file = "${tempJobDirectory.getAbsolutePath()}/testVariableWithNameAndScopeProject.zip"
                  overwrite=true
                }
           }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		def computedValue = """{"status":"success","exportfile":"${tempJobDirectory.getAbsolutePath()}/testVariableWithNameAndScopeProject.zip"}"""
		
		then:
		notThrown(Exception)
		assert new File("${tempJobDirectory.getAbsolutePath()}/testVariableWithNameAndScopeProject.zip").exists()
		result.output.contains(computedValue)
	}
	
	def "the task should export v1 all environments variables when include_all_var is true and names is set"() {
		given:
		
		File tempJobDirectory = File.createTempDir("project", ".tmp")
		
		enqueueRequest("[{\"id\":1,\"name\":\"MONGO_IP_TEST\",\"value\":\"192.168.1.1\",\"isPassword\":false,\"platformId\":1},{\"id\":2,\"name\":\"MONGO_PORT_TEST\",\"value\":\"77777\",\"isPassword\":false,\"platformId\":1}, {\"id\":3,\"name\":\"MONGO_IP_TEST_2\",\"value\":\"192.168.1.1\",\"isPassword\":false,\"platformId\":21}]")
		
		buildFile << """
           saagie {
                server {
                   url = 'http://localhost:9000/'
                    login = 'user'
                    password = 'password'
                    environment = 1
                    useLegacy = false
                }

                env {
                   include_all_var = true
                   scope = 'project'
                   name=['MONGO_IP', 'MONGO_PORT']
                }

                exportArtifacts {
                  export_file = "${tempJobDirectory.getAbsolutePath()}/testVariableWithNameAndScopeProject.zip"
                  overwrite=true
                }
           }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		def computedValue = """{"status":"success","exportfile":"${tempJobDirectory.getAbsolutePath()}/testVariableWithNameAndScopeProject.zip"}"""
		
		then:
		notThrown(Exception)
		assert new File("${tempJobDirectory.getAbsolutePath()}/testVariableWithNameAndScopeProject.zip").exists()
		result.output.contains(computedValue)
	}
	
	def "the task export v1 should fait if one of the names given in the name attribute doesn't exist"() {
		given:
		
		File tempJobDirectory = File.createTempDir("project", ".tmp")
		
		enqueueRequest("[{\"id\":1,\"name\":\"MONGO_IP_TEST\",\"value\":\"192.168.1.1\",\"isPassword\":false,\"platformId\":1},{\"id\":2,\"name\":\"MONGO_PORT_TEST\",\"value\":\"77777\",\"isPassword\":false,\"platformId\":1}, {\"id\":3,\"name\":\"MONGO_IP_TEST_2\",\"value\":\"192.168.1.1\",\"isPassword\":false,\"platformId\":21}]")
		
		buildFile << """
           saagie {
                server {
                   url = 'http://localhost:9000/'
                    login = 'user'
                    password = 'password'
                    environment = 1
                    useLegacy = false
                }

                env {
                   include_all_var = false
                   scope = 'project'
                   name=['NAME_DOESNT_EXIST', 'MONGO_PORT']
                }

                exportArtifacts {
                  export_file = "${tempJobDirectory.getAbsolutePath()}/testVariableWithNameAndScopeProject.zip"
                  overwrite=true
                }
           }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		then:
		UnexpectedBuildFailure e = thrown()
		result == null
		e.message.contains("Didn't find variable name: NAME_DOESNT_EXIST in the required environment variables list in V1")
		e.getBuildResult().task(":${taskName}").outcome == FAILED
	}
	
	
	
}
