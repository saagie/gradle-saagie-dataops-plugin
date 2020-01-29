package io.saagie.plugin.tasks.job

import io.saagie.plugin.DataOpsGradleTaskSpecification
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_IMPORT_JOB
import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('projectsImportJob task tests')
class JobImportTaskTest extends DataOpsGradleTaskSpecification {
    @Shared String taskName = PROJECTS_IMPORT_JOB
    @Shared ClassLoader classLoader = getClass().getClassLoader()
    @Shared String exportJobZipFilename = './exportedJob.zip'

    def "the task should fail if required params are not provided"() {
        given:
        buildFile << """
            saagie {
                server {
                    url = '${mockServerUrl}'
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

    def "the task should create a new job based on the exported config if name doesn't exist"() {
        given:
        URL resource = classLoader.getResource(exportJobZipFilename)
        File exportedConfig = new File(resource.getFile())

        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"name job"},{"id":"id-2","name":"name job 2"},{"id":"id-3","name":"name job 3"}]}}')
        enqueueRequest('{"data":{"createJob":{"id":"job-id","name":"Job from import"}}}')


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

                importJob {
                    import_file = '${exportedConfig.absolutePath}'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{"status":"success","id":"job-id"}')
    }

    def "the task should update a new job based on the exported config if name exist"() {
        given:
        URL resource = classLoader.getResource(exportJobZipFilename)
        File exportedConfig = new File(resource.getFile())

        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import"},{"id":"id-2","name":"name job 2"},{"id":"id-3","name":"name job 3"}]}}')
        enqueueRequest('{"data":{"editJob":{"id":"id-1"}}}')
        enqueueRequest('{"data":{"addJobVersion":{"number":"jobNumber"}}}')

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

                importJob {
                    import_file = '${exportedConfig.absolutePath}'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{"status":"success","id":"id-1"}')
    }

    def "the task should fail if the import_file does not exists"() {
        given:
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

                importJob {
                    import_file = 'invalid/path/test.zip'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains("Check that there is a file to upload in 'invalid/path/test.zip'. Be sure that 'invalid/path/test.zip' is a correct file path.")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }
}
