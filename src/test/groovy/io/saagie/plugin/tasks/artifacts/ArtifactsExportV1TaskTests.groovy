package io.saagie.plugin.tasks.artifacts

import io.saagie.plugin.DataOpsGradleTaskSpecification
import io.saagie.plugin.dataops.DataOpsModule
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('ArtifactsExportTaskTests task tests')
class ArtifactsExportV1TaskTests extends DataOpsGradleTaskSpecification {
    @Shared String taskName = DataOpsModule.PROJECTS_EXPORT_JOB_V1

    def "the task should export jobs and piplines with artifacts"() {
        given:

        File tempJob1File = File.createTempFile("package", ".tmp")
        File tempJob2File = File.createTempFile("package", ".tmp")
        File tempJob3File = File.createTempFile("package", ".tmp")
        File tempJobDirectory = File.createTempDir("project", ".tmp")

        enqueueRequest("{\"id\":\"pipeline-id\",\"name\":\"Analyze images with deepomatic\",\"schedule\":\"0 0 0 * * *\",\"createDate\":\"2018-04-16T16:26:39+0000\",\"modificationDate\":\"2020-04-22T08:38:21+0000\",\"platformId\":1,\"jobs\":[{\"id\":\"job-1\",\"name\":\"Deepomatic - import images\",\"capsule_code\":\"python\",\"category\":\"extract\",\"position\":0,\"current\":{\"number\":1}},{\"id\":\"job-2\",\"name\":\"Deepomatic - Cleaning images\",\"capsule_code\":\"java-scala\",\"category\":\"processing\",\"position\":1,\"current\":{\"number\":2}},{\"id\":\"job-3\",\"name\":\"Deepomatic - Analyse images\",\"capsule_code\":\"python\",\"category\":\"processing\",\"position\":2,\"current\":{\"number\":1}}],\"lastInstance\":{\"id\":\"last-instance-id\",\"startDate\":\"2020-05-22T00:00:00+0000\",\"endDate\":\"2020-05-22T00:02:05+0000\",\"workflowId\":\"pipeline-id\",\"status\":\"SUCCESS\"},\"runningInstances\":0}\n")
        enqueueRequest("{\"id\":\"job-1\",\"capsule_code\":\"docker\",\"current\":{\"isSubDomain\":true,\"subDomain\":\"****-technology\",\"url\":\"4-****-technology.prod.saagie.io\",\"isPort\":true,\"port\":80,\"id\":\"job-version-id\",\"job_id\":\"job-1\",\"number\":1,\"packageUrl\":\"morbz/docker:latest\",\"creation_date\":\"2017-11-12T12:53:18+00:00\",\"options\":[],\"isExternalSubDomain\":true,\"isInternalSubDomain\":false,\"externalSubDomain\":\"****-technology\",\"external_url\":\"4-****-technology.prod.saagie.io\",\"cpu\":0.2,\"memory\":256,\"disk\":128,\"isExternalPort\":true,\"isInternalPort\":false,\"externalPort\":80,\"enableAuth\":true,\"releaseNote\":\"\",\"important\":false},\"versions\":[{\"isSubDomain\":true,\"subDomain\":\"****-technology\",\"url\":\"4-****-technology.prod.saagie.io\",\"isPort\":true,\"port\":80,\"id\":\"job-version-id\",\"job_id\":\"job-1\",\"number\":1,\"packageUrl\":\"morbz/docker:latest\",\"creation_date\":\"2017-11-12T12:53:18+00:00\",\"options\":[],\"isExternalSubDomain\":true,\"isInternalSubDomain\":false,\"externalSubDomain\":\"****-technology\",\"external_url\":\"4-****-technology.prod.saagie.io\",\"cpu\":0.2,\"memory\":256,\"disk\":128,\"isExternalPort\":true,\"isInternalPort\":false,\"externalPort\":80,\"enableAuth\":true,\"releaseNote\":\"\",\"important\":false}],\"streaming\":true,\"category\":\"dataviz\",\"name\":\"**** Technology\",\"email\":\"\",\"always_email\":false,\"platform_id\":1,\"manual\":true,\"schedule\":\"R0/2017-11-12T12:52:13.451Z/P0Y0M1DT0H0M0S\",\"retry\":\"\",\"last_instance\":{\"id\":\"last-instance-id\",\"status\":\"KILLED\",\"version_number\":1,\"startDateTime\":\"2018-04-23T19:18:16+00:00\",\"endDateTime\":\"2018-05-18T12:40:07+00:00\"},\"last_state\":{\"id\":\"state-id\",\"state\":\"STOPPED\",\"date\":\"2018-05-18T12:40:07+00:00\",\"lastTaskStatus\":\"KILLED\",\"lastTaskId\":\"last-state-id\"},\"workflows\":[],\"deletable\":true}\n")
        enqueueRequest("{\"data\":{\"technologies\":[{\"id\":\"Docker-id\",\"label\":\"Docker\",\"isAvailable\":true},{\"id\":\"Java/Scala-id\",\"label\":\"Java/Scala\",\"isAvailable\":true},{\"id\":\"Python-id\",\"label\":\"Python\",\"isAvailable\":true},{\"id\":\"R-id\",\"label\":\"R\",\"isAvailable\":true},{\"id\":\"Spark-id\",\"label\":\"Spark\",\"isAvailable\":true},{\"id\":\"Sqoop-id\",\"laabel\":\"SQOOP\",\"isAvailable\":true},{\"id\":\"Talend-id\",\"label\":\"Talend\",\"isAvailable\":true},{\"id\":\"Bash-id\",\"label\":\"Bash\",\"isAvailable\":true}]}}")
        enqueueRequest("{\"data\":{\"technologiesVersions\":[]}}")
        enqueueRequest("{\"id\":\"job-2\",\"capsule_code\":\"python\",\"current\":{\"isSubDomain\":false,\"isPort\":false,\"id\":\"job-version-2\",\"job_id\":\"job-2\",\"number\":1,\"template\":\"python {file} arg1 arg2\",\"file\":\"deepomatic-import.py\",\"creation_date\":\"2018-03-06T09:30:20+00:00\",\"options\":{\"language_version\":\"3.5.2\"},\"isExternalSubDomain\":false,\"isInternalSubDomain\":false,\"cpu\":0.3,\"memory\":512,\"disk\":512,\"isExternalPort\":false,\"isInternalPort\":false,\"releaseNote\":\"\",\"important\":false},\"versions\":[{\"isSubDomain\":false,\"isPort\":false,\"id\":\"job-version-2\",\"job_id\":\"job-2\",\"number\":1,\"template\":\"python {file} arg1 arg2\",\"file\":\"deepomatic-import.py\",\"creation_date\":\"2018-03-06T09:30:20+00:00\",\"options\":{\"language_version\":\"3.5.2\"},\"isExternalSubDomain\":false,\"isInternalSubDomain\":false,\"cpu\":0.3,\"memory\":512,\"disk\":512,\"isExternalPort\":false,\"isInternalPort\":false,\"releaseNote\":\"\",\"important\":false}],\"streaming\":false,\"category\":\"extract\",\"name\":\"Deepomatic - import images\",\"email\":\"\",\"always_email\":false,\"platform_id\":1,\"manual\":true,\"schedule\":\"R0/2018-03-06T09:29:58.054Z/P0Y0M1DT0H0M0S\",\"retry\":\"\",\"last_instance\":{\"id\":\"last-instance-id\",\"status\":\"SUCCESS\",\"version_number\":1,\"startDateTime\":\"2020-05-22T00:00:03+00:00\",\"endDateTime\":\"2020-05-22T00:00:25+00:00\"},\"last_state\":{\"id\":\"last-state-id\",\"state\":\"STOPPED\",\"date\":\"2020-05-22T00:00:24+00:00\",\"lastTaskStatus\":\"SUCCESS\",\"lastTaskId\":\"last-track-id\"},\"workflows\":[{\"inCurrent\":true,\"id\":\"workflow-1\",\"name\":\"Deepomatic - Reconnaissance d'images\",\"lastInstanceStatus\":\"SUCCESS\",\"instancesRunningWithJob\":[],\"runningInstances\":0},{\"inCurrent\":true,\"id\":\"workflow-2\",\"name\":\"Analyze images with deepomatic\",\"lastInstanceStatus\":\"SUCCESS\",\"instancesRunningWithJob\":[],\"runningInstances\":0}],\"deletable\":false}")
        enqueueRequest("{\"data\":{\"technologiesVersions\":[{\"versionLabel\":\"2.7.15\",\"technologyLabel\":\"Python\"},{\"versionLabel\":\"3.6.6\",\"technologyLabel\":\"Python\"},{\"versionLabel\":\"3.5\",\"technologyLabel\":\"Python\"},{\"versionLabel\":\"2.7\",\"technologyLabel\":\"Python\"},{\"versionLabel\":\"3.6\",\"technologyLabel\":\"Python\"},{\"versionLabel\":\"3.7\",\"technologyLabel\":\"Python\"}]}}")
        enqueueRequest("{\"id\":\"job-3\",\"capsule_code\":\"java-scala\",\"current\":{\"isSubDomain\":false,\"isPort\":false,\"id\":\"job-version-2\",\"job_id\":\"job-3\",\"number\":2,\"template\":\"java -cp {file} ImageCleaner\",\"file\":\"deepomatic-cleaning.jar\",\"creation_date\":\"2018-03-06T09:32:55+00:00\",\"options\":{\"language_version\":\"8.131\"},\"isExternalSubDomain\":false,\"isInternalSubDomain\":false,\"cpu\":0.3,\"memory\":512,\"disk\":512,\"isExternalPort\":false,\"isInternalPort\":false,\"releaseNote\":\"\",\"important\":false},\"versions\":[{\"isSubDomain\":false,\"isPort\":false,\"id\":\"job-version-1\",\"job_id\":\"job-3\",\"number\":1,\"template\":\"java -jar {file} arg1 arg2\",\"file\":\"deepomatic-cleaning.jar\",\"creation_date\":\"2018-03-06T09:31:07+00:00\",\"options\":{\"language_version\":\"8.131\"},\"isExternalSubDomain\":false,\"isInternalSubDomain\":false,\"cpu\":0.3,\"memory\":512,\"disk\":512,\"isExternalPort\":false,\"isInternalPort\":false,\"releaseNote\":\"\",\"important\":false},{\"isSubDomain\":false,\"isPort\":false,\"id\":\"job-version-2\",\"job_id\":\"job-3\",\"number\":2,\"template\":\"java -cp {file} ImageCleaner\",\"file\":\"deepomatic-cleaning.jar\",\"creation_date\":\"2018-03-06T09:32:55+00:00\",\"options\":{\"language_version\":\"8.131\"},\"isExternalSubDomain\":false,\"isInternalSubDomain\":false,\"cpu\":0.3,\"memory\":512,\"disk\":512,\"isExternalPort\":false,\"isInternalPort\":false,\"releaseNote\":\"\",\"important\":false}],\"streaming\":false,\"category\":\"processing\",\"name\":\"Deepomatic - Cleaning images\",\"email\":\"\",\"always_email\":false,\"platform_id\":4,\"manual\":true,\"schedule\":\"R0/2018-03-06T09:30:46.170Z/P0Y0M1DT0H0M0S\",\"retry\":\"\",\"last_instance\":{\"id\":\"last-instance-1\",\"status\":\"SUCCESS\",\"version_number\":2,\"startDateTime\":\"2020-05-22T00:00:27+00:00\",\"endDateTime\":\"2020-05-22T00:01:44+00:00\"},\"last_state\":{\"id\":\"last-state-id\",\"state\":\"STOPPED\",\"date\":\"2020-05-22T00:01:43+00:00\",\"lastTaskStatus\":\"SUCCESS\",\"lastTaskId\":\"last-trask-id\"},\"workflows\":[{\"inCurrent\":true,\"id\":299,\"name\":\"Deepomatic - Reconnaissance d'images\",\"lastInstanceStatus\":\"SUCCESS\",\"instancesRunningWithJob\":[],\"runningInstances\":0},{\"inCurrent\":true,\"id\":331,\"name\":\"Analyze images with deepomatic\",\"lastInstanceStatus\":\"SUCCESS\",\"instancesRunningWithJob\":[],\"runningInstances\":0}],\"deletable\":false}")
        enqueueRequest("{\"data\":{\"technologiesVersions\":[{\"versionLabel\":\"8\",\"technologyLabel\":\"Java/Scala\"},{\"versionLabel\":\"7\",\"technologyLabel\":\"Java/Scala\"},{\"versionLabel\":\"11\",\"technologyLabel\":\"Java/Scala\"}]}}")
        enqueueRequest("{\"id\":\"job-4\",\"capsule_code\":\"python\",\"current\":{\"isSubDomain\":false,\"isPort\":false,\"id\":\"job-version-1\",\"job_id\":\"job-4\",\"number\":1,\"template\":\"python {file} arg1 arg2\",\"file\":\"deepomatic-analyze.py\",\"creation_date\":\"2018-03-06T09:31:37+00:00\",\"options\":{\"language_version\":\"3.5.2\"},\"isExternalSubDomain\":false,\"isInternalSubDomain\":false,\"cpu\":0.3,\"memory\":512,\"disk\":512,\"isExternalPort\":false,\"isInternalPort\":false,\"releaseNote\":\"\",\"important\":false},\"versions\":[{\"isSubDomain\":false,\"isPort\":false,\"id\":\"job-version-1\",\"job_id\":\"job-4\",\"number\":1,\"template\":\"python {file} arg1 arg2\",\"file\":\"deepomatic-analyze.py\",\"creation_date\":\"2018-03-06T09:31:37+00:00\",\"options\":{\"language_version\":\"3.5.2\"},\"isExternalSubDomain\":false,\"isInternalSubDomain\":false,\"cpu\":0.3,\"memory\":512,\"disk\":512,\"isExternalPort\":false,\"isInternalPort\":false,\"releaseNote\":\"\",\"important\":false}],\"streaming\":false,\"category\":\"processing\",\"name\":\"Deepomatic - Analyse images\",\"email\":\"\",\"always_email\":false,\"platform_id\":1,\"manual\":true,\"schedule\":\"R0/2018-03-06T09:31:17.677Z/P0Y0M1DT0H0M0S\",\"retry\":\"\",\"last_instance\":{\"id\":\"last-instance-id\",\"status\":\"SUCCESS\",\"version_number\":1,\"startDateTime\":\"2020-05-22T00:01:59+00:00\",\"endDateTime\":\"2020-05-22T00:02:05+00:00\"},\"last_state\":{\"id\":\"last-state-id\",\"state\":\"STOPPED\",\"date\":\"2020-05-22T00:02:05+00:00\",\"lastTaskStatus\":\"SUCCESS\",\"lastTaskId\":\"last-task-id\"},\"workflows\":[{\"inCurrent\":true,\"id\":\"workflow-id-1\",\"name\":\"Deepomatic - Reconnaissance d'images\",\"lastInstanceStatus\":\"SUCCESS\",\"instancesRunningWithJob\":[],\"runningInstances\":0},{\"inCurrent\":true,\"id\":\"workflow-id-2\",\"name\":\"Analyze images with deepomatic\",\"lastInstanceStatus\":\"SUCCESS\",\"instancesRunningWithJob\":[],\"runningInstances\":0}],\"deletable\":false}")
        enqueueRequestFile(tempJob1File)
        enqueueRequestFile(tempJob2File)
        enqueueRequestFile(tempJob3File)
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
