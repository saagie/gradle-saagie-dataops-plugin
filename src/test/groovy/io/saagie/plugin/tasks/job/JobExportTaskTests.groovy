package io.saagie.plugin.tasks.job

import io.saagie.plugin.DataOpsGradleTaskSpecification
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_EXPORT_JOB;
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('JobExportTaskTests task tests')
class JobExportTaskTests extends DataOpsGradleTaskSpecification {
    @Shared String taskName = PROJECTS_EXPORT_JOB

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

    def "the task should fail if the export_file_path does not exists"() {
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

                export {
                    export_file_path = 'invalide/path/directory'
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
                    id = 'job-id'
                }

                export {
                    export_file_path = '${tempJobDirectory.getAbsolutePath()}'
                    overwrite = true 
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)
        then:
        notThrown(Exception)
        assert new File("${tempJobDirectory.getAbsolutePath()}/project-export-project-id.zip").exists()
        result.output.contains('{"status":true,"exportfile":"'+ tempJobDirectory.getAbsolutePath()+'/project-export-project-id.zip"}')

    }

}
