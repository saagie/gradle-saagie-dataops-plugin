package io.saagie.plugin.tasks.artifacts

import io.saagie.plugin.DataOpsGradleTaskSpecification
import io.saagie.plugin.dataops.DataOpsModule
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Title

import static org.gradle.testkit.runner.TaskOutcome.FAILED

@Title('projectsImportJob task tests')
class ArtifactsImportTaskTest extends DataOpsGradleTaskSpecification {
    @Shared
    String taskName = DataOpsModule.PROJECTS_IMPORT_ARTIFACTS_JOB
    @Shared
    ClassLoader classLoader = getClass().getClassLoader()
    @Shared
    String exportJobZipFilename = './exportedJob.zip'
    @Shared
    String exportPipelineWithoutJobZipFilename = './exportedPipelineWithoutJob.zip'
    @Shared
    String exportJobWithoutPipelineZipFilename = './exportedJobWithoutPipeline.zip'
    @Shared
    String exportJobJustJobVersionWithoutPipelineZipFilename = './exportJobJustJobVersionWithoutPipelineZipFilename.zip'
    @Shared
    String exportAppZipFilename = './exportedApp.zip'
    @Shared
    String exportAppWithAppVersionZipFilename = './exportedAppWithAppVersion.zip'



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

                importArtifacts {
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

    def "the task should create a new job and update pipline with new version based on the exported config if name doesn exist"() {
        given:
        URL resource = classLoader.getResource(exportJobZipFilename)
        File exportedConfig = new File(resource.getFile())
        enqueueRequest('{"data":{"repositories":[{"id":"id-tech-1","name":"Saagie","technologies":[{"id":"id-tech-2","label":"tech-2","description":"description-1","icon":"icon-1","backgroundColor":null,"available":true,"customFlags":["flag-1"]},{"id":"id-tech-3","label":"tech-3","description":"des-3","icon":"icon-3","backgroundColor":"#E87A35","available":true,"customFlags":[]},{"id":"id-tech-4","label":"tech-4","description":"desc-4","icon":"icon-4","backgroundColor":"#728E9B","available":true,"customFlags":[]}]}]}}')
        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"name job 2"},{"id":"id-3","name":"name job 3"}]}}')
        enqueueRequest('{"data":{"createJob":{"id":"id-1","name":"Job from import"}}}')
        enqueueRequest('{"data":{"pipelines":[{"id":"id-1","name":"Test 2"},{"id":"id-2","name":"Test Long"},{"id":"id-3","name":"test pipeline"},{"id":"id-4","name":"test pipeline 23"},{"id":"id-5","name":"test pipeline id 5"}]}}')
        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"name job 2"},{"id":"id-3","name":"name job 3"}]}}')
        enqueueRequest('{"data":{"addPipelineVersion":{"number":2}}}')

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

                jobOverride {
                    isScheduled = true
                    cronScheduling = false
                    alerting {
                        emails= ['amine@bearstudio.fr']
                        statusList= []
                    }
                }

                importArtifacts {
                    import_file = '${exportedConfig.absolutePath}'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{status=success, job=[{id=id-1, name=Test Job3 imported from file}], pipeline=[{id=id-1, name=test pipeline 23}], variable=[], app=[]}')
    }

    def "the task should create a new pipeline and add new version to another pipeline without job based on the exported config"() {
        given:
        URL resource = classLoader.getResource(exportPipelineWithoutJobZipFilename)
        File exportedConfig = new File(resource.getFile())
        enqueueRequest('{"data":{"repositories":[{"id":"id-tech-1","name":"Saagie","technologies":[{"id":"id-tech-2","label":"tech-2","description":"description-1","icon":"icon-1","backgroundColor":null,"available":true,"customFlags":["flag-1"]},{"id":"id-tech-3","label":"tech-3","description":"des-3","icon":"icon-3","backgroundColor":"#E87A35","available":true,"customFlags":[]},{"id":"id-tech-4","label":"tech-4","description":"desc-4","icon":"icon-4","backgroundColor":"#728E9B","available":true,"customFlags":[]}]}]}}')
        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"test added job"},{"id":"id-3","name":"name job 3"}]}}')
        enqueueRequest('{"data":{"pipelines":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"name exist"},{"id":"id-3","name":"name job 3"}, {"id": "id-4", "name": "test added job"}, {"id": "id-5", "name": "test pipeline exist"}]}}')
        enqueueRequest('{"data":{"addPipelineVersion":{"number":2}}}')
        enqueueRequest('{"data":{"pipelines":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"name exist"},{"id":"id-3","name":"name job 3"}, {"id": "id-4", "name": "test added job"}, {"id": "id-5", "name": "test pipeline exist"}]}}')
        enqueueRequest('{"data":{"createPipeline":{"id":"id-1","name":"test pipeline 23"}}}')

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
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{status=success, job=[], pipeline=[{id=id-2, name=test pipeline exist}, {id=id-1, name=test pipeline 23}], variable=[], app=[]}')
    }




    def "the task should create new app and add new version to another app based on the exported config"() {
        given:
        URL resource = classLoader.getResource(exportAppZipFilename)
        File exportedConfig = new File(resource.getFile())
        enqueueRequest('{"data":{"repositories":[{"id":"id-tech-1","name":"Saagie","technologies":[{"id":"id-tech-2","label":"tech-2","description":"description-1","icon":"icon-1","backgroundColor":null,"available":true,"customFlags":["flag-1"]},{"id":"id-tech-3","label":"tech-3","description":"des-3","icon":"icon-3","backgroundColor":"#E87A35","available":true,"customFlags":[]},{"id":"id-tech-4","label":"tech-4","description":"desc-4","icon":"icon-4","backgroundColor":"#728E9B","available":true,"customFlags":[]}]}]}}')
        enqueueRequest('{"data":{"labWebApps":[{"id":"id-app-1","name":"testImport8"},{"id":"id-app-2","name":"testImport7"},{"id":"id-app-3","name":"testImport6"},{"id":"id-app-4","name":"testImport5"},{"id":"id-app-5","name":"testImport4"},{"id":"id-app-6","name":"testImport3"},{"id":"id-app-7","name":"testImport2"},{"id":"id-app-8","name":"testImport"}]}}')
        enqueueRequest('{"data":{"createJob":{"id":"id-app-10","name":"testImport9"}}}')
        enqueueRequest('{"data":{"addJobVersion":{"number":2}}}')

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
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{status=success, job=[], pipeline=[], variable=[], app=[{id=id-1, name=testImport9, versions=2}]}')
    }


    def "the task should add appVersion based on the build configuration if name exist"() {
        given:
        URL resource = classLoader.getResource(exportAppWithAppVersionZipFilename)
        File exportedConfig = new File(resource.getFile())
        enqueueRequest('{"data":{"repositories":[{"id":"id-tech-1","name":"Saagie","technologies":[{"id":"id-tech-2","label":"tech-1","description":"description-1","icon":"icon-1","backgroundColor":null,"available":true,"customFlags":["flag-1"]},{"id":"id-tech-3","label":"tech-2","description":"des-3","icon":"icon-3","backgroundColor":"#E87A35","available":true,"customFlags":[]},{"id":"id-tech-4","label":"tech-4","description":"desc-4","icon":"icon-4","backgroundColor":"#728E9B","available":true,"customFlags":[]}]}]}}')
        enqueueRequest('{"data":{"labWebApps":[{"id":"id-app-1","name":"app-1"},{"id":"id-app-2","name":"testImport7"},{"id":"id-app-3","name":"testImport6"},{"id":"id-app-4","name":"testImport5"},{"id":"id-app-5","name":"testImport4"},{"id":"id-app-6","name":"testImport3"},{"id":"id-app-7","name":"testImport2"},{"id":"id-app-8","name":"testImport"}]}}')
        enqueueRequest('{"data":{"addJobVersion":{"number":2}}}')

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
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{status=success, job=[], pipeline=[], variable=[], app=[{id=id-1, name=app-1}]}')
    }


    def "the task should create a new pipeline and new job based on the exported config if name doesn't exist"() {
        given:
        URL resource = classLoader.getResource(exportJobZipFilename)
        File exportedConfig = new File(resource.getFile())
        enqueueRequest('{"data":{"repositories":[{"id":"id-tech-1","name":"Saagie","technologies":[{"id":"id-tech-2","label":"tech-1","description":"description-1","icon":"icon-1","backgroundColor":null,"available":true,"customFlags":["flag-1"]},{"id":"id-tech-3","label":"tech-3","description":"des-3","icon":"icon-3","backgroundColor":"#E87A35","available":true,"customFlags":[]},{"id":"id-tech-4","label":"tech-4","description":"desc-4","icon":"icon-4","backgroundColor":"#728E9B","available":true,"customFlags":[]}]}]}}')
        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"name job 2"},{"id":"id-3","name":"name job 3"}]}}')
        enqueueRequest('{"data":{"createJob":{"id":"id-1","name":"Job from import"}}}')
        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"name job 2"},{"id":"id-3","name":"name job 3"}]}}')
        enqueueRequest('{"data":{"pipelines":[{"id":"id-1","name":"Test 2"},{"id":"id-2","name":"Test Long"},{"id":"id-3","name":"test pipeline"},{"id":"id-4","name":"test pipeline id 3"},{"id":"id-5","name":"test pipeline id 5"}]}}')
        enqueueRequest('{"data":{"createPipeline":{"id":"id-1","name":"test pipeline 23"}}}')

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
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{status=success, job=[{id=id-1, name=Test Job3 imported from file}], pipeline=[{id=id-1, name=test pipeline 23}], variable=[], app=[]}')
    }


    def "the task should add jobVersion based on the build configuration if name exist with overwrite"() {

        given:
        URL resource = classLoader.getResource(exportJobJustJobVersionWithoutPipelineZipFilename)
        File exportedConfig = new File(resource.getFile())
        enqueueRequest('{"data":{"repositories":[{"id":"id-tech-1","name":"Saagie","technologies":[{"id":"id-tech-2","label":"tech-1","description":"description-1","icon":"icon-1","backgroundColor":null,"available":true,"customFlags":["flag-1"]},{"id":"id-tech-3","label":"tech-3","description":"des-3","icon":"icon-3","backgroundColor":"#E87A35","available":true,"customFlags":[]},{"id":"id-tech-4","label":"tech-4","description":"desc-4","icon":"icon-4","backgroundColor":"#728E9B","available":true,"customFlags":[]}]}]}}')
        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"test added job"},{"id":"id-3","name":"name job 3"}]}}')
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

                importArtifacts {
                    import_file = '${exportedConfig.absolutePath}'
                }
            }
        """

        when:
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{status=success, job=[{id=id-1, name=test added job}], pipeline=[], variable=[], app=[]}')
    }


    def "the task should create new job and add new version to another job without pipeline based on the exported config"() {
        given:
        URL resource = classLoader.getResource(exportJobWithoutPipelineZipFilename)
        File exportedConfig = new File(resource.getFile())
        enqueueRequest('{"data":{"repositories":[{"id":"id-tech-1","name":"Saagie","technologies":[{"id":"id-tech-2","label":"tech-1","description":"description-1","icon":"icon-1","backgroundColor":null,"available":true,"customFlags":["flag-1"]},{"id":"id-tech-3","label":"tech-3","description":"des-3","icon":"icon-3","backgroundColor":"#E87A35","available":true,"customFlags":[]},{"id":"id-tech-4","label":"tech-4","description":"desc-4","icon":"icon-4","backgroundColor":"#728E9B","available":true,"customFlags":[]}]}]}}')
        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"test added job"},{"id":"id-3","name":"name job 3"}]}}')
        enqueueRequest('{"data":{"createJob":{"id":"job-id","name":"Job from import"}}}')
        enqueueRequest('{"data":{"jobs":[{"id":"id-1","name":"Job from import asdas"},{"id":"id-2","name":"name exist"},{"id":"id-3","name":"name job 3"}, {"id": "id-4", "name": "test added job"}]}}')
        enqueueRequest('{"data":{"addJobVersion":{"number":2}}}')

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
        BuildResult result = gradle(taskName)

        then:
        notThrown(Exception)
        result.output.contains('{status=success, job=[{id=id-2, name=name exist}, {id=id-1, name=test added job}], pipeline=[], variable=[], app=[]}')
    }
    def "the task should fail if the app's technology does not exists"() {

        given:
        URL resource = classLoader.getResource(exportAppWithAppVersionZipFilename)
        File exportedConfig = new File(resource.getFile())
        enqueueRequest('{"data":{"repositories":[{"id":"id-tech-1","name":"Saagie","technologies":[{"id":"id-tech-2","label":"tech-1","description":"description-1","icon":"icon-1","backgroundColor":null,"available":true,"customFlags":["flag-1"]},{"id":"id-tech-3","label":"tech-3","description":"des-3","icon":"icon-3","backgroundColor":"#E87A35","available":true,"customFlags":[]},{"id":"id-tech-4","label":"tech-4","description":"desc-4","icon":"icon-4","backgroundColor":"#728E9B","available":true,"customFlags":[]}]}]}}')
        enqueueRequest('{"data":{"labWebApps":[{"id":"id-app-1","name":"app-1"},{"id":"id-app-2","name":"testImport7"},{"id":"id-app-3","name":"testImport6"},{"id":"id-app-4","name":"testImport5"},{"id":"id-app-5","name":"testImport4"},{"id":"id-app-6","name":"testImport3"},{"id":"id-app-7","name":"testImport2"},{"id":"id-app-8","name":"testImport"}]}}')
        enqueueRequest('{"data":{"addJobVersion":{"number":2}}}')

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
        BuildResult result = gradle(taskName)

        then:
        UnexpectedBuildFailure e = thrown()
        result == null
        e.message.contains("Technology with name tech-2 is not available on the targeted server")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }



}
