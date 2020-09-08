package io.saagie.plugin.tasks.artifacts

import io.saagie.plugin.DataOpsGradleTaskSpecification
import io.saagie.plugin.dataops.DataOpsModule
import org.gradle.testkit.runner.BuildResult
import spock.lang.Shared
import spock.lang.Title

@Title('ArtifactsExportTaskTests task tests')
class ArtifactsExportV1TaskTests extends DataOpsGradleTaskSpecification {
	@Shared
	String taskName = DataOpsModule.PROJECTS_EXPORT_ARTIFACTS_V1
	
	def "the task should export docker job without artifact and without technologie run version"() {
		given:
		
		File tempJobDirectory = File.createTempDir("project", ".tmp")
		
		enqueueRequest("{\"id\":\"job-id\",\"capsule_code\":\"docker\",\"current\":{\"isSubDomain\":true,\"subDomain\":\"xxxxx-technology\",\"url\":\"xxxx4-technology.prod.saagie.io\",\"isPort\":true,\"port\":80,\"id\":\"job-version-id\",\"job_id\":\"job-id\",\"number\":1,\"packageUrl\":\"morb/docker-web:latest\",\"creation_date\":\"2017-11-12T12:53:18+00:00\",\"options\":[],\"isExternalSubDomain\":true,\"isInternalSubDomain\":false,\"externalSubDomain\":\"xxxxx-technology\",\"external_url\":\"4-xxxxx-technology.prod.saagie.io\",\"cpu\":0.2,\"memory\":256,\"disk\":128,\"isExternalPort\":true,\"isInternalPort\":false,\"externalPort\":80,\"enableAuth\":true,\"releaseNote\":\"\",\"important\":false},\"versions\":[{\"isSubDomain\":true,\"subDomain\":\"xxxxx-technology\",\"url\":\"4-xxxxx-technology.prod.saagie.io\",\"isPort\":true,\"port\":80,\"id\":\"job-version-id\",\"job_id\":\"job-id\",\"number\":1,\"packageUrl\":\"morbz/docker-web:latest\",\"creation_date\":\"2017-11-12T12:53:18+00:00\",\"options\":[],\"isExternalSubDomain\":true,\"isInternalSubDomain\":false,\"externalSubDomain\":\"xxxxx-technology\",\"external_url\":\"4-xxxxx-technology.prod.saagie.io\",\"cpu\":0.2,\"memory\":256,\"disk\":128,\"isExternalPort\":true,\"isInternalPort\":false,\"externalPort\":80,\"enableAuth\":true,\"releaseNote\":\"\",\"important\":false}],\"streaming\":true,\"category\":\"dataviz\",\"name\":\"job name\",\"email\":\"\",\"always_email\":false,\"platform_id\":1,\"manual\":true,\"schedule\":\"R0/2017-11-12T12:52:13.451Z/P0Y0M1DT0H0M0S\",\"retry\":\"\",\"last_instance\":{\"id\":\"instance-id\",\"status\":\"KILLED\",\"version_number\":\"version-nomber\",\"startDateTime\":\"2018-04-23T19:18:16+00:00\",\"endDateTime\":\"2018-05-18T12:40:07+00:00\"},\"last_state\":{\"id\":\"last-instance-id\",\"state\":\"STOPPED\",\"date\":\"2018-05-18T12:40:07+00:00\",\"lastTaskStatus\":\"KILLED\",\"lastTaskId\":\"last-instance-id\"},\"workflows\":[],\"deletable\":true}")
		enqueueRequest("{\"data\":{\"technologies\":[{\"id\":\"Docker-id\",\"label\":\"Generic\",\"isAvailable\":true},{\"id\":\"Java/Scala-id\",\"label\":\"Java/Scala\",\"isAvailable\":true},{\"id\":\"Python-id\",\"label\":\"Python\",\"isAvailable\":true},{\"id\":\"R-id\",\"label\":\"R\",\"isAvailable\":true},{\"id\":\"Spark-id\",\"label\":\"Spark\",\"isAvailable\":true},{\"id\":\"Sqoop-id\",\"label\":\"SQOOP\",\"isAvailable\":true},{\"id\":\"Talend-id\",\"label\":\"Talend\",\"isAvailable\":true},{\"id\":\"Bash-id\",\"label\":\"Bash\",\"isAvailable\":true}]}}")
		enqueueRequest("{\"data\":{\"technologiesVersions\":[{\"id\":\"Generic_Docker\",\"technologyLabel\":\"Generic\",\"versionLabel\":\"Docker\"}]}}")
		buildFile << """
           saagie {
                server {
                   url = 'http://localhost:9000/'
                    login = 'user'
                    password = 'password'
                    environment = 1
                    useLegacy = false
                }

                job {
                   ids = ['job-id']
                }

                exportArtifacts {
                  export_file = "${tempJobDirectory.getAbsolutePath()}/testdocker1.zip"
                  overwrite=true
                }
           }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		def computedValue = """{"status":"success","exportfile":"${tempJobDirectory.getAbsolutePath()}/testdocker1.zip"}"""
		
		then:
		notThrown(Exception)
		assert new File("${tempJobDirectory.getAbsolutePath()}/testdocker1.zip").exists()
		result.output.contains(computedValue)
		tempJobDirectory.deleteDir()
	}
}
