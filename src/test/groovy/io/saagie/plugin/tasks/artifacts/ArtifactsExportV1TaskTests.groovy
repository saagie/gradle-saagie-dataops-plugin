package io.saagie.plugin.tasks.artifacts

import io.saagie.plugin.DataOpsGradleTaskSpecification
import io.saagie.plugin.dataops.DataOpsModule
import org.gradle.testkit.runner.BuildResult
import spock.lang.Shared
import spock.lang.Title

@Title('ArtifactsExportTaskTests task tests')
class ArtifactsExportV1TaskTests extends DataOpsGradleTaskSpecification {
	@Shared
	String taskName = DataOpsModule.PROJECTS_EXPORT_JOB_V1
	
	
	def "the task export v1 jobs"() {
		given:
		buildFile << """
           saagie {
                server {
                    url = "https://saagie-workspace.prod.saagie.io/"
                    login ="mohamed.amin.ziraoui"
                    password = "1!@#qweASD"
                    environment = 4
                    jwt = true
                    acceptSelfSigned = true
                }

                job {
                   ids = ['3736']
                   include_all_versions = true
                   
                }
                
                pipeline {
                   ids = ['402']
                   include_job = true
                   include_all_versions = true
                }

                exportArtifacts {
                   export_file = "/home/amine/Desktop/test_gradle/exportTestWithIncludejobs.zip"
                   overwrite=true
                }
           }
        """
		when:
		BuildResult result = gradle(taskName)
		then:
		notThrown(Exception)
		assert true == true
	}
	
	def "the task should export jobs and piplines with artifacts"() {
		given:
		File tempJobDirectory = File.createTempDir("project", ".tmp")
		File tempJobFile = File.createTempFile("package", ".tmp")
		
		enqueueRequest("""{"data":{"job":{"id":"job-id","name":"Test Job to archive","description":"Description","countJobInstance":5,"versions":[{"number":1,"creationDate":"2019-11-05T17:14:08.635Z","releaseNote":"Fixed release","runtimeVersion":"3.6","packageInfo":{"downloadUrl":"/projects/api/platform/4/project/project-id/job/job-id/version/1/artifact/renan-file.py"},"dockerInfo":null,"commandLine":"python {file} arg1 arg2","isCurrent":true,"isMajor":false,"creator":"admin.user"}],"category":"Extraction","technology":{"id":"technology-id","label":"Python","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-11-05T17:14:08.635Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":false}}}""")
		enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"name job 2"},{"id":"id-3","name":"name job 3"}]}}')
		enqueueRequestFile(tempJobFile)
		
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

                job {
                   ids = ['job-4']
                }

                pipeline {
                   ids = ['pipeline-id']
                   include_job = true
                }

                exportArtifacts {
                  export_file = "${tempJobDirectory.getAbsolutePath()}/testExportIncludedJobs.zip"
                  overwrite=true
                }
           }
        """
		
		when:
		BuildResult result = gradle(taskName)
		
		def computedValue = """{"status":"success","exportfile":"${tempJobDirectory.getAbsolutePath()}/testExportIncludedJobs.zip"}"""
		
		then:
		notThrown(Exception)
		assert new File("${tempJobDirectory.getAbsolutePath()}/testExportIncludedJobs.zip").exists()
		result.output.contains(computedValue)
		tempJobDirectory.deleteDir()
		tempJob1File.delete()
		tempJob2File.delete()
		tempJob3File.delete()
	}
	
	def "the task should export docker job without artifact and without technologie run version"() {
		given:
		
		File tempJobDirectory = File.createTempDir("project", ".tmp")
		
		enqueueRequest("{\"id\":\"job-id\",\"capsule_code\":\"docker\",\"current\":{\"isSubDomain\":true,\"subDomain\":\"xxxxx-technology\",\"url\":\"xxxx4-technology.prod.saagie.io\",\"isPort\":true,\"port\":80,\"id\":\"job-version-id\",\"job_id\":\"job-id\",\"number\":1,\"packageUrl\":\"morb/docker-web:latest\",\"creation_date\":\"2017-11-12T12:53:18+00:00\",\"options\":[],\"isExternalSubDomain\":true,\"isInternalSubDomain\":false,\"externalSubDomain\":\"xxxxx-technology\",\"external_url\":\"4-xxxxx-technology.prod.saagie.io\",\"cpu\":0.2,\"memory\":256,\"disk\":128,\"isExternalPort\":true,\"isInternalPort\":false,\"externalPort\":80,\"enableAuth\":true,\"releaseNote\":\"\",\"important\":false},\"versions\":[{\"isSubDomain\":true,\"subDomain\":\"xxxxx-technology\",\"url\":\"4-xxxxx-technology.prod.saagie.io\",\"isPort\":true,\"port\":80,\"id\":\"job-version-id\",\"job_id\":\"job-id\",\"number\":1,\"packageUrl\":\"morbz/docker-web:latest\",\"creation_date\":\"2017-11-12T12:53:18+00:00\",\"options\":[],\"isExternalSubDomain\":true,\"isInternalSubDomain\":false,\"externalSubDomain\":\"xxxxx-technology\",\"external_url\":\"4-xxxxx-technology.prod.saagie.io\",\"cpu\":0.2,\"memory\":256,\"disk\":128,\"isExternalPort\":true,\"isInternalPort\":false,\"externalPort\":80,\"enableAuth\":true,\"releaseNote\":\"\",\"important\":false}],\"streaming\":true,\"category\":\"dataviz\",\"name\":\"job name\",\"email\":\"\",\"always_email\":false,\"platform_id\":1,\"manual\":true,\"schedule\":\"R0/2017-11-12T12:52:13.451Z/P0Y0M1DT0H0M0S\",\"retry\":\"\",\"last_instance\":{\"id\":\"instance-id\",\"status\":\"KILLED\",\"version_number\":\"version-nomber\",\"startDateTime\":\"2018-04-23T19:18:16+00:00\",\"endDateTime\":\"2018-05-18T12:40:07+00:00\"},\"last_state\":{\"id\":\"last-instance-id\",\"state\":\"STOPPED\",\"date\":\"2018-05-18T12:40:07+00:00\",\"lastTaskStatus\":\"KILLED\",\"lastTaskId\":\"last-instance-id\"},\"workflows\":[],\"deletable\":true}")
		enqueueRequest("{\"data\":{\"technologies\":[{\"id\":\"Docker-id\",\"label\":\"Docker\",\"isAvailable\":true},{\"id\":\"Java/Scala-id\",\"label\":\"Java/Scala\",\"isAvailable\":true},{\"id\":\"Python-id\",\"label\":\"Python\",\"isAvailable\":true},{\"id\":\"R-id\",\"label\":\"R\",\"isAvailable\":true},{\"id\":\"Spark-id\",\"label\":\"Spark\",\"isAvailable\":true},{\"id\":\"Sqoop-id\",\"laabel\":\"SQOOP\",\"isAvailable\":true},{\"id\":\"Talend-id\",\"label\":\"Talend\",\"isAvailable\":true},{\"id\":\"Bash-id\",\"label\":\"Bash\",\"isAvailable\":true}]}}")
		enqueueRequest("{\"data\":{\"technologiesVersions\":[]}}")
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
