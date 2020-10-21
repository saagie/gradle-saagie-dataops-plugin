package io.saagie.plugin.tasks.artifacts

import io.saagie.plugin.DataOpsGradleTaskSpecification
import io.saagie.plugin.dataops.DataOpsModule
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('JobExportTaskTests task tests')
class ArtifactsExportTaskTests extends DataOpsGradleTaskSpecification {
    @Shared
    String taskName = DataOpsModule.PROJECTS_EXPORT_ARTIFACTS

    def "the task should fail if required params are not provided"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'login'
                    password = 'password'
                    environment = 1
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains("Missing params in plugin configuration: https://github.com/saagie/gradle-saagie-dataops-plugin/wiki/${taskName}")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
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
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains("configuration export path does not exist")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
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
        BuildResult result = gradle(taskName, "-d")

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

                pipeline {
                    ids = ['pipeline-1', 'pipeline-2']
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


    def "projectsExportJob should export app and app versions"() {
        given:
        File tempAppDirectory = File.createTempDir("project", ".tmp")
        enqueueRequest("""{"data":{"labWebApp":{"id":"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx","name":"appname","description":"description","creationDate":"2020-10-15T08:24:31.747Z","isDeletable":true,"storageSizeInMB":68,"instances":[{"id":"35da0b4b-db64-4fc0-94cb-2b80c1afa3e3","status":"KILLED","statusDetails":null,"startTime":"2020-10-21T17:42:00.393Z","endTime":"2020-10-21T17:42:18.006Z","version":{"number":2}}],"versions":[{"number":2,"creator":"create_name","creationDate":"2020-10-21T17:41:59.327Z","isCurrent":true,"releaseNote":"Release note","dockerInfo":{"image":"dockervalidimage2","dockerCredentialsId":null},"exposedPorts":[{"name":"webapp","port":8080,"isAuthenticationRequired":false,"isRewriteUrl":true,"basePathVariableName":"app_path"},{"name":"weappoptional","port":4000,"isAuthenticationRequired":true,"isRewriteUrl":true,"basePathVariableName":null}],"storagePaths":["/test3","/test4"]},{"number":1,"creator":"creator.name","creationDate":"2020-10-15T08:24:31.747Z","isCurrent":false,"releaseNote":"release note message","dockerInfo":{"image":"dockervalidimage","dockerCredentialsId":null},"exposedPorts":[{"name":"portname","port":8080,"isAuthenticationRequired":true,"isRewriteUrl":true,"basePathVariableName":"port_name"}],"storagePaths":["/test","/test2"]}],"alerting":{"emails":["email.email@email.com"],"statusList":["SUCCEEDED","FAILED","REQUESTED","QUEUED","KILLING"],"loginEmails":[]},"technology":{"id":"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx","label":"Docker image"}}}}""")
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

                apps {
                    ids = ["xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"]
                    include_all_versions = true
                }

                exportArtifacts {
                    export_file = '${tempAppDirectory.getAbsolutePath()}/zipappfile.zip'
                    overwrite = true
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        def computedValue = """{"status":"success","exportfile":"${tempAppDirectory.getAbsolutePath()}/zipappfile.zip"}"""

        then:
        notThrown(Exception)
        assert new File("${tempAppDirectory.getAbsolutePath()}/zipappfile.zip").exists()
        result.output.contains(computedValue)
        tempAppDirectory.deleteDir()
    }

    def "projectsExport should fail if app id doesn't exist"() {
        given:
        File tempAppDirectory = File.createTempDir("project", ".tmp")
        enqueueRequest("""{"data": {"labWebApp": null}}""")
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

                apps {
                    ids = ["xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"]
                    include_all_versions = true
                }

                exportArtifacts {
                    export_file = '${tempAppDirectory.getAbsolutePath()}/zipappfile.zip'
                    overwrite = true
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        def computedValue = """{"status":"success","exportfile":"${tempAppDirectory.getAbsolutePath()}/zipappfile.zip"}"""

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains("App with id xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx does not exist")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

    def "projectsExport should throw error if not app configuration provided"() {
        given:
        File tempAppDirectory = File.createTempDir("project", ".tmp")
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
                    export_file = '${tempAppDirectory.getAbsolutePath()}/zipappfile.zip'
                    overwrite = true
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains("jobs, pipelines,variables and apps to be exported cannot be empty at the same time, and cannot generate zip file")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

    def "projectsExportJob should export job, job version, pipeLines, pipeline versions and environment variables"() {
        given:
        File tempJobDirectory = File.createTempDir("project", ".tmp")
        File tempJobFile = File.createTempFile("package", ".tmp")
        enqueueRequest("""{"data":{"pipelines":[{"id":"pipeline-1","name":"pipeline-1","description":"pipeline-1","versions":[{"number":1,"creationDate":"2019-11-06T07:49:27.235Z","releaseNote":null,"jobs":[{"id":"job-1"},{"id":"job-3"}],"isCurrent":true,"isMajor":false,"creator":"youen.chene"}],"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null},{"id":"pipeline-2","name":"pipeline-2","description":"pipeline-2","versions":[{"number":1,"creationDate":"2019-11-06T07:49:27.235Z","releaseNote":null,"jobs":[{"id":"job-4"},{"id":"job-5"}],"isCurrent":true,"isMajor":false,"creator":"youen.chene"}],"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null}]}}""")
        enqueueRequest("""{"data":{"jobs":[{"id":"job-2","name":"Test Job to archive2","description":"Description","countJobInstance":5,"versions":[{"number":2,"creationDate":"2019-11-05T17:14:08.635Z","releaseNote":"Fixed release","runtimeVersion":"3.6","packageInfo":{"downloadUrl":"/projects/api/platform/4/project/project-id/job/job-id/version/1/artifact/renan-file.py"},"dockerInfo":null,"commandLine":"python {file} arg1 arg2","isCurrent":true,"isMajor":false,"creator":"admin.user"},{"number":1,"creationDate":"2019-11-05T17:14:08.635Z","releaseNote":"Fixed release 2","runtimeVersion":"3.6","packageInfo":{"downloadUrl":"/projects/api/platform/4/project/project-id/job/job-id/version/1/artifact/renan-file.py"},"dockerInfo":null,"commandLine":"python {file} arg1 arg2","isCurrent":true,"isMajor":false,"creator":"admin.user"}],"category":"Extraction","technology":{"id":"technology-id","label":"Python","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-11-05T17:14:08.635Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":false}, {"id":"job-1","name":"Test Job to archive2","description":"Description","countJobInstance":5,"versions":[{"number":1,"creationDate":"2019-11-05T17:14:08.635Z","releaseNote":"Fixed release","runtimeVersion":"3.6","packageInfo":{"downloadUrl":"/projects/api/platform/4/project/project-id/job/job-id/version/1/artifact/renan-file.py"},"dockerInfo":null,"commandLine":"python {file} arg1 arg2","isCurrent":true,"isMajor":false,"creator":"admin.user"}],"category":"Extraction","technology":{"id":"technology-id","label":"Python","isAvailable":true},"isScheduled":false,"cronScheduling":null,"scheduleStatus":null,"alerting":null,"isStreaming":false,"creationDate":"2019-11-05T17:14:08.635Z","migrationStatus":null,"migrationProjectId":null,"isDeletable":false}]}}""")
        enqueueRequest("{\"data\":{\"projectEnvironmentVariables\":[{\"id\":\"project-environment-1-id\",\"scope\":\"GLOBAL\",\"name\":\"testUX2\",\"value\":\"lala1\",\"description\":\"lala1\",\"isPassword\":false,\"overriddenValues\":[]},{\"id\":\"project-environment-2-id\",\"scope\":\"GLOBAL\",\"name\":\"sjones\",\"value\":null,\"description\":\"description\",\"isPassword\":true,\"overriddenValues\":[]},{\"id\":\"project-environment-3-id\",\"scope\":\"PROJECT\",\"name\":\"globalvar\",\"value\":\"pro\",\"description\":\"pro\",\"isPassword\":false,\"overriddenValues\":[{\"id\":\"project-environment-4-id\",\"scope\":\"GLOBAL\",\"value\":\"GlobalEnvVAr\",\"description\":\"global variable\",\"isPassword\":false}]},{\"id\":\"project-environment-5-id\",\"scope\":\"GLOBAL\",\"name\":\"bbbb\",\"value\":\"bbbbb\",\"description\":\"\",\"isPassword\":false,\"overriddenValues\":[]},{\"id\":\"project-environment-6-id\",\"scope\":\"GLOBAL\",\"name\":\"WEBHDFS_URL\",\"value\":null,\"description\":\"\",\"isPassword\":true,\"overriddenValues\":[]}]}}")
        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"name job 2"},{"id":"id-3","name":"name job 3"}]}}')
        enqueueRequestFile(tempJobFile)
        enqueueRequestFile(tempJobFile)
        enqueueRequestFile(tempJobFile)
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
                    include_all_artifacts = true
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
