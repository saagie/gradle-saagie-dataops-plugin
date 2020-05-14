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

    def "the task should fail if required params are not provided"() {
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

                project {
                    id = '2438b9b6-a9ee-4816-bfa8-9ed89896dfb4'
                }

                job {
                   ids = ['9999']
                }

                pipeline {
                   ids = ['486']
                }

                exportArtifacts {
                  export_file = "/home/amine/Desktop/test_gradle/export.zip"
                  overwrite=true
                  temporary_directory='/tmp'
                }
           }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        assert true == true
    }

    def "the task should fail if the export_file does not exists"() {
        given:
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
                    id = 'job-id'
                }

                exportArtifacts {
                    export_file = 'invalide/path/directory.zip'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        assert true == true
    }

    def "projectsExportJob should export job and job version"() {
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
                    ids = ['job-id']
                }

                exportArtifacts {
                    export_file = '${tempJobDirectory.getAbsolutePath()}/zipfile.zip'
                    overwrite = true
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        def computedValue = """{"status":"success","exportfile":"${tempJobDirectory.getAbsolutePath()}/zipfile.zip"}"""

        then:
        notThrown(Exception)
        assert new File("${tempJobDirectory.getAbsolutePath()}/zipfile.zip").exists()
        result.output.contains(computedValue)
        tempJobDirectory.deleteDir()
        tempJobFile.delete()
    }

    def "projectsExportJob should export only pipeLines"() {
        given:
        File tempJobDirectory = File.createTempDir("project", ".tmp")
        File tempJobFile = File.createTempFile("package", ".tmp")

        enqueueRequest("""{"data":{"pipeline":{"id":"pipeline-1","name":"pipeline-1","description":"pipeline-1","versions":[{"number":1,"creationDate":"2019-11-06T07:49:27.235Z","releaseNote":null,"jobs":[],"isCurrent":true,"isMajor":false,"creator":"youen.chene"}],"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null}}}""")
        enqueueRequest("""{"data":{"pipeline":{"id":"pipeline-2","name":"pipeline-2","description":"pipeline-2","versions":[{"number":1,"creationDate":"2019-11-06T07:49:27.235Z","releaseNote":null,"jobs":[],"isCurrent":true,"isMajor":false,"creator":"youen.chene"}],"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null}}}""")
        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"name job 2"},{"id":"id-3","name":"name job 3"}]}}')

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
                    export_file = '${tempJobDirectory.getAbsolutePath()}/zipfile.zip'

                }
            }
        """

        when:
        BuildResult result = gradle(taskName, '-d')

        def computedValue = """{"status":"success","exportfile":"${tempJobDirectory.getAbsolutePath()}/zipfile.zip"}"""

        then:
        notThrown(Exception)
        assert new File("${tempJobDirectory.getAbsolutePath()}/zipfile.zip").exists()
        result.output.contains(computedValue)
        tempJobDirectory.deleteDir()
        tempJobFile.delete()
    }

    def "projectsExportJob should export job, job version and pipeLines with include_job true"() {
        given:
        File tempJobDirectory = File.createTempDir("project", ".tmp")
        File tempJobFile = File.createTempFile("package", ".tmp")

        enqueueRequest("""{"data":{"pipeline":{"id":"pipeline-1","name":"pipeline-1","description":"pipeline-1","versions":[{"number":1,"creationDate":"2019-11-06T07:49:27.235Z","releaseNote":null,"jobs":[{"id":"job-1"},{"id":"job-2"}],"isCurrent":true,"isMajor":false,"creator":"youen.chene"}],"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null}}}""")
        enqueueRequest("""{"data":{"pipeline":{"id":"pipeline-2","name":"pipeline-2","description":"pipeline-2","versions":[{"number":1,"creationDate":"2019-11-06T07:49:27.235Z","releaseNote":null,"jobs":[{"id":"job-1"}],"isCurrent":true,"isMajor":false,"creator":"youen.chene"}],"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null}}}""")
        enqueueRequest("""{"data":{"job":{"id":"job-1","name":"Test Job to archive1","description":"Description","countJobInstance":5,"versions":[{"number":1,"creationDate":"2019-11-05T17:14:08.635Z","releaseNote":"Fixed release","runtimeVersion":"3.6","packageInfo":{"downloadUrl":"/home/amine/projects/saggie/gradle-saagie-dataops-plugin/src/test/resources/exportedJob/Job/d936c1d5-86e9-4268-b65a-82e17b344046/package/job.py"},"dockerInfo":null,"commandLine":"python {file} arg1 arg2","isCurrent":true,"isMajor":false,"creator":"admin.user"}],"category":"Extraction","technology":{"id":"technology-id","label":"Python","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-11-05T17:14:08.635Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":false}}}""")
        enqueueRequest("""{"data":{"job":{"id":"job-2","name":"Test Job to archive2","description":"Description","countJobInstance":5,"versions":[{"number":1,"creationDate":"2019-11-05T17:14:08.636Z","releaseNote":"Fixed release","runtimeVersion":"3.6","packageInfo":{"downloadUrl":"/home/amine/projects/saggie/gradle-saagie-dataops-plugin/src/test/resources/exportedJob/Job/d936c1d5-86e9-4268-b65a-82e17b344046/package/job.py"},"dockerInfo":null,"commandLine":"python {file} arg1 arg2","isCurrent":true,"isMajor":false,"creator":"admin.user"}],"category":"Extraction","technology":{"id":"technology-id","label":"Python","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-11-05T17:14:08.635Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":false}}}""")
        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"name job 2"},{"id":"id-3","name":"name job 3"}]}}')
        enqueueRequestFile(tempJobFile)
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
                    ids = ['job-1', 'job-2']
                }

                pipeline {
                    ids = ['pipeline-1', 'pipeline-2']
                    include_job = true
                }

                exportArtifacts {
                    export_file = '${tempJobDirectory.getAbsolutePath()}/zipfile.zip'
                    overwrite = true
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        def computedValue = """{"status":"success","exportfile":"${tempJobDirectory.getAbsolutePath()}/zipfile.zip"}"""

        then:
        notThrown(Exception)
        assert new File("${tempJobDirectory.getAbsolutePath()}/zipfile.zip").exists()
        result.output.contains(computedValue)
        tempJobDirectory.deleteDir()
        tempJobFile.delete()
    }

    def "projectsExportJob should export job, job version and pipeLines with include_job false (default)"() {
        given:
        File tempJobDirectory = File.createTempDir("project", ".tmp")
        File tempJobFile = File.createTempFile("package", ".tmp")
        enqueueRequest("""{"data":{"pipeline":{"id":"pipeline-1","name":"pipeline-1","description":"pipeline-1","versions":[{"number":1,"creationDate":"2019-11-06T07:49:27.235Z","releaseNote":null,"jobs":[{"id":"job-1"},{"id":"job-3"}],"isCurrent":true,"isMajor":false,"creator":"youen.chene"}],"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null}}}""")
        enqueueRequest("""{"data":{"pipeline":{"id":"pipeline-2","name":"pipeline-2","description":"pipeline-2","versions":[{"number":1,"creationDate":"2019-11-06T07:49:27.235Z","releaseNote":null,"jobs":[{"id":"job-4"},{"id":"job-5"}],"isCurrent":true,"isMajor":false,"creator":"youen.chene"}],"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null}}}""")
        enqueueRequest("""{"data":{"job":{"id":"job-2","name":"Test Job to archive2","description":"Description","countJobInstance":5,"versions":[{"number":1,"creationDate":"2019-11-05T17:14:08.635Z","releaseNote":"Fixed release","runtimeVersion":"3.6","packageInfo":{"downloadUrl":"/projects/api/platform/4/project/project-id/job/job-id/version/1/artifact/renan-file.py"},"dockerInfo":null,"commandLine":"python {file} arg1 arg2","isCurrent":true,"isMajor":false,"creator":"admin.user"}],"category":"Extraction","technology":{"id":"technology-id","label":"Python","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-11-05T17:14:08.635Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":false}}}""")
        enqueueRequest("""{"data":{"job":{"id":"job-1","name":"Test Job to archive2","description":"Description","countJobInstance":5,"versions":[{"number":1,"creationDate":"2019-11-05T17:14:08.635Z","releaseNote":"Fixed release","runtimeVersion":"3.6","packageInfo":{"downloadUrl":"/projects/api/platform/4/project/project-id/job/job-id/version/1/artifact/renan-file.py"},"dockerInfo":null,"commandLine":"python {file} arg1 arg2","isCurrent":true,"isMajor":false,"creator":"admin.user"}],"category":"Extraction","technology":{"id":"technology-id","label":"Python","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-11-05T17:14:08.635Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":false}}}""")
        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"name job 2"},{"id":"id-3","name":"name job 3"}]}}')
        enqueueRequestFile(tempJobFile)
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
                    ids = ['job-1', 'job-2']
                }

                pipeline {
                    ids = ['pipeline-1', 'pipeline-2']
                }

                exportArtifacts {
                    export_file = '${tempJobDirectory.getAbsolutePath()}/zipfile.zip'
                    overwrite = true
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        def computedValue = """{"status":"success","exportfile":"${tempJobDirectory.getAbsolutePath()}/zipfile.zip"}"""

        then:
        notThrown(Exception)
        assert new File("${tempJobDirectory.getAbsolutePath()}/zipfile.zip").exists()
        result.output.contains(computedValue)
        tempJobDirectory.deleteDir()
        tempJobFile.delete()
    }
}
