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
        result.output.contains('{status=success, job=[{id=id-1, name=Test Job3 imported from file}], pipeline=[{id=id-1, name=test pipeline 23}], variable=[]}')
    }

    def "the task should create a new pipeline and add new version to another pipeline without job based on the exported config"() {
        given:
        URL resource = classLoader.getResource(exportPipelineWithoutJobZipFilename)
        File exportedConfig = new File(resource.getFile())
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
        result.output.contains('{status=success, job=[], pipeline=[{id=id-2, name=test pipeline exist}, {id=id-1, name=test pipeline 23}], variable=[]}')
    }

    def "the task should create a new pipeline and new job based on the exported config if name doesn't exist"() {
        given:
        URL resource = classLoader.getResource(exportJobZipFilename)
        File exportedConfig = new File(resource.getFile())
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
        result.output.contains('{status=success, job=[{id=id-1, name=Test Job3 imported from file}], pipeline=[{id=id-1, name=test pipeline 23}], variable=[]}')
    }

    def "the task should create new job and add new version to another job without pipeline based on the exported config"() {
        given:
        URL resource = classLoader.getResource(exportJobWithoutPipelineZipFilename)
        File exportedConfig = new File(resource.getFile())
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
        result.output.contains('{status=success, job=[{id=id-2, name=name exist}, {id=id-1, name=test added job}], pipeline=[], variable=[]}')
    }

    def "the task should add jobVersion based on the build configuration if name exist with overwrite"() {

        given:
        URL resource = classLoader.getResource(exportJobJustJobVersionWithoutPipelineZipFilename)
        File exportedConfig = new File(resource.getFile())
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
        result.output.contains('{status=success, job=[{id=id-1, name=test added job}], pipeline=[], variable=[]}')
    }


    def "the task should create new app and add new version to another app based on the exported config"() {
        given:
        URL resource = classLoader.getResource(exportAppZipFilename)
        File exportedConfig = new File(resource.getFile())
        enqueueRequest('{"data":{"repositories":[{"id":"9fcbddfe-a7b7-4d25-807c-ad030782c923","name":"Saagie","technologies":[{},{},{},{},{},{},{},{"id":"36912c68-d084-43b9-9fda-b5ded8eb7b13","label":"Docker image","description":"Use any Docker image.","icon":"generic-app","backgroundColor":null,"available":true,"customFlags":["GenericApp"]},{},{"id":"7d3f247c-b5a9-4a34-a0a2-f6b209bc2b63","label":"Jupyter Notebook","description":"The Jupyter Notebook is an open-source web application that allows you to create and share documents that contain live code, equations, visualization and narrative text. Uses include: data cleaning and transformation, numerical simulation, statistical modeling, data visualization, machine learning and much more.","icon":"jupyter","backgroundColor":"#E87A35","available":true,"customFlags":[]},{"id":"5dbc03b5-c947-4c8e-874a-1c580687a9a8","label":"Nifi","description":"An easy to use, powerful, and reliable system to process and distribute data.","icon":"nifi","backgroundColor":"#728E9B","available":true,"customFlags":[]},{"id":"1227b1dd-ad4c-4627-ab92-832b8c67e05d","label":"RStudio 3.4.2","description":"RStudio is an integrated development environment (IDE) for R. It includes a console, syntax-highlighting editor that supports direct code execution, as well as tools for plotting, history, debugging and workspace management.","icon":"rstudio","backgroundColor":"#3374BA","available":true,"customFlags":[]},{"id":"e93e461d-1c85-4107-9292-d8f88fbce1ee","label":"RStudio 3.6.2","description":"RStudio is an integrated development environment (IDE) for R. It includes a console, syntax-highlighting editor that supports direct code execution, as well as tools for plotting, history, debugging and workspace management.","icon":"rstudio","backgroundColor":"#3374BA","available":true,"customFlags":[]},{"id":"1c072060-7f0d-410e-aa51-372fbf26b3e9","label":"Zeppelin Notebook 0.9.0","description":"Web-based notebook that enables data-driven, interactive data analytics and collaborative documents with SQL, Scala and more.","icon":"zeppelin","backgroundColor":"#0099CC","available":true,"customFlags":[]},{"id":"06190567-1fb9-4ede-9a65-ff4b1a226b27","label":"Zeppelin Notebook","description":"Web-based notebook that enables data-driven, interactive data analytics and collaborative documents with SQL, Scala and more.","icon":"zeppelin","backgroundColor":"#0099CC","available":true,"customFlags":[]}]},{"id":"3aa9aada-bd97-42d7-88ee-ed384210762c","name":"Service","technologies":[{},{"id":"f1a99521-8b7f-4451-a0e8-4f63a67f4b61","label":"Grafana","description":"Grafana allows you to query, visualize, alert on and understand your metrics no matter where they are stored. Create, explore, and share dashboards with your team and foster a data driven culture.","icon":"grafana","backgroundColor":"#1857B8","available":true,"customFlags":[]},{"id":"e405405b-723e-45b2-b33c-e6c33ccb0805","label":"TTYD - Interactive Bash","description":"TTYD is a simple command-line tool for sharing terminal over the web. It is repackaged for Saagie including hadoop/hdfs, beeline, sqoop and spark (only in local) command lines.","icon":"bash","backgroundColor":"#979ba1","available":true,"customFlags":[]},{"id":"6ca8b860-7aeb-4928-9b93-2405a56e14d3","label":"Dash","description":"Dash empowers teams to build data science and ML apps that put the power of Python, R, and Julia in the hands of business users.","icon":"datascience","backgroundColor":"#20293d","available":true,"customFlags":[]},{"id":"c54bc19a-e473-4883-becc-8b5a43b989d2","label":"Spark History Server","description":"The Spark history server is a monitoring tool that displays information about completed Spark applications","icon":"spark","backgroundColor":"#e25a1c","available":true,"customFlags":[]}]},{"id":"fa01ba8a-9de7-48c1-9917-6bd43bc201ec","name":"Augustin","technologies":[{}]},{"id":"50a1bd16-70d7-441b-8f50-d469ff7bda4e","name":"Test-Qiwei","technologies":[{},{"id":"85191392-10ef-4ed9-91f2-9feca96c1bb2","label":"Grafana","description":"Grafana allows you to query, visualize, alert on and understand your metrics no matter where they are stored. Create, explore, and share dashboards with your team and foster a data driven culture.","icon":"grafana","backgroundColor":"#1857B8","available":true,"customFlags":[]},{"id":"28345b88-623c-4d2d-9340-9be55f46a5d5","label":"Mlflow-server","description":"MLflow tracking server is used for logging parameters, code versions, metrics, and output files when running your machine learning code and for later visualizing the results.","icon":"generic-app","backgroundColor":"#092241","available":true,"customFlags":[]},{"id":"109b8a4a-d8cf-4924-a3c5-92820369c410","label":"TTYD - Interactive Bash","description":"TTYD is a simple command-line tool for sharing terminal over the web. It is repackaged for Saagie including hadoop/hdfs, beeline, sqoop and spark (only in local) command lines.","icon":"bash","backgroundColor":"#979ba1","available":true,"customFlags":[]},{"id":"a9430810-d139-4b3b-9f05-58b974efbb6a","label":"Dash","description":"Dash empowers teams to build data science and ML apps that put the power of Python, R, and Julia in the hands of business users.","icon":"datascience","backgroundColor":"#20293d","available":true,"customFlags":[]},{"id":"4bff8629-2cc3-4f63-99bf-f2302167f58e","label":"Spark History Server","description":"The Spark history server is a monitoring tool that displays information about completed Spark applications","icon":"spark","backgroundColor":"#e25a1c","available":true,"customFlags":[]}]},{"id":"1865e9fd-adfc-46b7-a395-c3b0267e197e","name":"Nicolas C","technologies":[{}]}]}}')
        enqueueRequest('{"data":{"labWebApps":[{"id":"e9aab7ec-bab0-44be-bfc9-a9fe3efd0659","name":"testImport9"},{"id":"ad1b837b-0da7-4fcb-abd1-ba0a82539324","name":"testImport8"},{"id":"110ee233-3bb8-4693-bcb4-10c485681d43","name":"testImport7"},{"id":"6a940a01-ff10-4124-bd57-cb645659d843","name":"testImport6"},{"id":"a971c2ff-a77d-4fe0-b358-90c42fcc69d6","name":"testImport5"},{"id":"bedbcf6e-85a6-4dc5-b165-f2f3b2d22027","name":"testImport4"},{"id":"a06dbcf5-8cc9-44af-bd5f-6037ca4af250","name":"testImport3"},{"id":"db68aec6-a426-418c-8d7c-d5c7ad608a84","name":"testImport2"},{"id":"3290b0c2-8b90-4055-98e2-c12e92f3ca1e","name":"testImport"},{"id":"23430e58-e6d7-4921-91ca-bef79e7a53c1","name":"tetttttt222222"},{"id":"5a4ef2a5-ba05-43a7-974e-b4784ccadd7b","name":"testwww"}]}}')
        enqueueRequest('{"data":{"createJob":{"id":"ba380936-613a-4301-b788-a307135cd1b7","name":"testImport10"}}}')
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
        enqueueRequest('{"data":{"repositories":[{"id":"9fcbddfe-a7b7-4d25-807c-ad030782c923","name":"Saagie","technologies":[{},{},{},{},{},{},{},{"id":"36912c68-d084-43b9-9fda-b5ded8eb7b13","label":"Docker image","description":"Use any Docker image.","icon":"generic-app","backgroundColor":null,"available":true,"customFlags":["GenericApp"]},{},{"id":"7d3f247c-b5a9-4a34-a0a2-f6b209bc2b63","label":"Jupyter Notebook","description":"The Jupyter Notebook is an open-source web application that allows you to create and share documents that contain live code, equations, visualization and narrative text. Uses include: data cleaning and transformation, numerical simulation, statistical modeling, data visualization, machine learning and much more.","icon":"jupyter","backgroundColor":"#E87A35","available":true,"customFlags":[]},{"id":"5dbc03b5-c947-4c8e-874a-1c580687a9a8","label":"Nifi","description":"An easy to use, powerful, and reliable system to process and distribute data.","icon":"nifi","backgroundColor":"#728E9B","available":true,"customFlags":[]},{"id":"1227b1dd-ad4c-4627-ab92-832b8c67e05d","label":"RStudio 3.4.2","description":"RStudio is an integrated development environment (IDE) for R. It includes a console, syntax-highlighting editor that supports direct code execution, as well as tools for plotting, history, debugging and workspace management.","icon":"rstudio","backgroundColor":"#3374BA","available":true,"customFlags":[]},{"id":"e93e461d-1c85-4107-9292-d8f88fbce1ee","label":"RStudio 3.6.2","description":"RStudio is an integrated development environment (IDE) for R. It includes a console, syntax-highlighting editor that supports direct code execution, as well as tools for plotting, history, debugging and workspace management.","icon":"rstudio","backgroundColor":"#3374BA","available":true,"customFlags":[]},{"id":"1c072060-7f0d-410e-aa51-372fbf26b3e9","label":"Zeppelin Notebook 0.9.0","description":"Web-based notebook that enables data-driven, interactive data analytics and collaborative documents with SQL, Scala and more.","icon":"zeppelin","backgroundColor":"#0099CC","available":true,"customFlags":[]},{"id":"06190567-1fb9-4ede-9a65-ff4b1a226b27","label":"Zeppelin Notebook","description":"Web-based notebook that enables data-driven, interactive data analytics and collaborative documents with SQL, Scala and more.","icon":"zeppelin","backgroundColor":"#0099CC","available":true,"customFlags":[]}]},{"id":"3aa9aada-bd97-42d7-88ee-ed384210762c","name":"Service","technologies":[{},{"id":"f1a99521-8b7f-4451-a0e8-4f63a67f4b61","label":"Grafana","description":"Grafana allows you to query, visualize, alert on and understand your metrics no matter where they are stored. Create, explore, and share dashboards with your team and foster a data driven culture.","icon":"grafana","backgroundColor":"#1857B8","available":true,"customFlags":[]},{"id":"e405405b-723e-45b2-b33c-e6c33ccb0805","label":"TTYD - Interactive Bash","description":"TTYD is a simple command-line tool for sharing terminal over the web. It is repackaged for Saagie including hadoop/hdfs, beeline, sqoop and spark (only in local) command lines.","icon":"bash","backgroundColor":"#979ba1","available":true,"customFlags":[]},{"id":"6ca8b860-7aeb-4928-9b93-2405a56e14d3","label":"Dash","description":"Dash empowers teams to build data science and ML apps that put the power of Python, R, and Julia in the hands of business users.","icon":"datascience","backgroundColor":"#20293d","available":true,"customFlags":[]},{"id":"c54bc19a-e473-4883-becc-8b5a43b989d2","label":"Spark History Server","description":"The Spark history server is a monitoring tool that displays information about completed Spark applications","icon":"spark","backgroundColor":"#e25a1c","available":true,"customFlags":[]}]},{"id":"fa01ba8a-9de7-48c1-9917-6bd43bc201ec","name":"Augustin","technologies":[{}]},{"id":"50a1bd16-70d7-441b-8f50-d469ff7bda4e","name":"Test-Qiwei","technologies":[{},{"id":"85191392-10ef-4ed9-91f2-9feca96c1bb2","label":"Grafana","description":"Grafana allows you to query, visualize, alert on and understand your metrics no matter where they are stored. Create, explore, and share dashboards with your team and foster a data driven culture.","icon":"grafana","backgroundColor":"#1857B8","available":true,"customFlags":[]},{"id":"28345b88-623c-4d2d-9340-9be55f46a5d5","label":"Mlflow-server","description":"MLflow tracking server is used for logging parameters, code versions, metrics, and output files when running your machine learning code and for later visualizing the results.","icon":"generic-app","backgroundColor":"#092241","available":true,"customFlags":[]},{"id":"109b8a4a-d8cf-4924-a3c5-92820369c410","label":"TTYD - Interactive Bash","description":"TTYD is a simple command-line tool for sharing terminal over the web. It is repackaged for Saagie including hadoop/hdfs, beeline, sqoop and spark (only in local) command lines.","icon":"bash","backgroundColor":"#979ba1","available":true,"customFlags":[]},{"id":"a9430810-d139-4b3b-9f05-58b974efbb6a","label":"Dash","description":"Dash empowers teams to build data science and ML apps that put the power of Python, R, and Julia in the hands of business users.","icon":"datascience","backgroundColor":"#20293d","available":true,"customFlags":[]},{"id":"4bff8629-2cc3-4f63-99bf-f2302167f58e","label":"Spark History Server","description":"The Spark history server is a monitoring tool that displays information about completed Spark applications","icon":"spark","backgroundColor":"#e25a1c","available":true,"customFlags":[]}]},{"id":"1865e9fd-adfc-46b7-a395-c3b0267e197e","name":"Nicolas C","technologies":[{}]}]}}')
        enqueueRequest('{"data":{"labWebApps":[{"id":"ba380936-613a-4301-b788-a307135cd1b7","name":"app-1"},{"id":"e9aab7ec-bab0-44be-bfc9-a9fe3efd0659","name":"testImport9"},{"id":"ad1b837b-0da7-4fcb-abd1-ba0a82539324","name":"testImport8"},{"id":"110ee233-3bb8-4693-bcb4-10c485681d43","name":"testImport7"},{"id":"6a940a01-ff10-4124-bd57-cb645659d843","name":"testImport6"},{"id":"a971c2ff-a77d-4fe0-b358-90c42fcc69d6","name":"testImport5"},{"id":"bedbcf6e-85a6-4dc5-b165-f2f3b2d22027","name":"testImport4"},{"id":"a06dbcf5-8cc9-44af-bd5f-6037ca4af250","name":"testImport3"},{"id":"db68aec6-a426-418c-8d7c-d5c7ad608a84","name":"testImport2"},{"id":"3290b0c2-8b90-4055-98e2-c12e92f3ca1e","name":"testImport"},{"id":"23430e58-e6d7-4921-91ca-bef79e7a53c1","name":"tetttttt222222"},{"id":"5a4ef2a5-ba05-43a7-974e-b4784ccadd7b","name":"testwww"}]}}')
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

    def "the task should fail if the app's technology does not exists"() {

        given:
        URL resource = classLoader.getResource(exportAppWithAppVersionZipFilename)
        File exportedConfig = new File(resource.getFile())
        enqueueRequest('{"data":{"repositories":[{"id":"9fcbddfe-a7b7-4d25-807c-ad030782c923","name":"Saagie","technologies":[{},{},{},{},{},{},{},{"id":"36912c68-d084-43b9-9fda-b5ded8eb7b13","label":"Docker image","description":"Use any Docker image.","icon":"generic-app","backgroundColor":null,"available":true,"customFlags":["GenericApp"]},{},{"id":"7d3f247c-b5a9-4a34-a0a2-f6b209bc2b63","label":"Jupyter Notebook","description":"The Jupyter Notebook is an open-source web application that allows you to create and share documents that contain live code, equations, visualization and narrative text. Uses include: data cleaning and transformation, numerical simulation, statistical modeling, data visualization, machine learning and much more.","icon":"jupyter","backgroundColor":"#E87A35","available":true,"customFlags":[]},{"id":"5dbc03b5-c947-4c8e-874a-1c580687a9a8","label":"Nifi","description":"An easy to use, powerful, and reliable system to process and distribute data.","icon":"nifi","backgroundColor":"#728E9B","available":true,"customFlags":[]},{"id":"1227b1dd-ad4c-4627-ab92-832b8c67e05d","label":"RStudio 3.4.2","description":"RStudio is an integrated development environment (IDE) for R. It includes a console, syntax-highlighting editor that supports direct code execution, as well as tools for plotting, history, debugging and workspace management.","icon":"rstudio","backgroundColor":"#3374BA","available":true,"customFlags":[]},{"id":"e93e461d-1c85-4107-9292-d8f88fbce1ee","label":"RStudio 3.6.2","description":"RStudio is an integrated development environment (IDE) for R. It includes a console, syntax-highlighting editor that supports direct code execution, as well as tools for plotting, history, debugging and workspace management.","icon":"rstudio","backgroundColor":"#3374BA","available":true,"customFlags":[]},{"id":"1c072060-7f0d-410e-aa51-372fbf26b3e9","label":"Zeppelin Notebook 0.9.0","description":"Web-based notebook that enables data-driven, interactive data analytics and collaborative documents with SQL, Scala and more.","icon":"zeppelin","backgroundColor":"#0099CC","available":true,"customFlags":[]},{"id":"06190567-1fb9-4ede-9a65-ff4b1a226b27","label":"Zeppelin Notebook","description":"Web-based notebook that enables data-driven, interactive data analytics and collaborative documents with SQL, Scala and more.","icon":"zeppelin","backgroundColor":"#0099CC","available":true,"customFlags":[]}]},{"id":"3aa9aada-bd97-42d7-88ee-ed384210762c","name":"Service","technologies":[{},{"id":"f1a99521-8b7f-4451-a0e8-4f63a67f4b61","label":"Grafana","description":"Grafana allows you to query, visualize, alert on and understand your metrics no matter where they are stored. Create, explore, and share dashboards with your team and foster a data driven culture.","icon":"grafana","backgroundColor":"#1857B8","available":true,"customFlags":[]},{"id":"e405405b-723e-45b2-b33c-e6c33ccb0805","label":"TTYD - Interactive Bash","description":"TTYD is a simple command-line tool for sharing terminal over the web. It is repackaged for Saagie including hadoop/hdfs, beeline, sqoop and spark (only in local) command lines.","icon":"bash","backgroundColor":"#979ba1","available":true,"customFlags":[]},{"id":"6ca8b860-7aeb-4928-9b93-2405a56e14d3","label":"Dash","description":"Dash empowers teams to build data science and ML apps that put the power of Python, R, and Julia in the hands of business users.","icon":"datascience","backgroundColor":"#20293d","available":true,"customFlags":[]},{"id":"c54bc19a-e473-4883-becc-8b5a43b989d2","label":"Spark History Server","description":"The Spark history server is a monitoring tool that displays information about completed Spark applications","icon":"spark","backgroundColor":"#e25a1c","available":true,"customFlags":[]}]},{"id":"fa01ba8a-9de7-48c1-9917-6bd43bc201ec","name":"Augustin","technologies":[{}]},{"id":"50a1bd16-70d7-441b-8f50-d469ff7bda4e","name":"Test-Qiwei","technologies":[{},{"id":"85191392-10ef-4ed9-91f2-9feca96c1bb2","label":"Grafana","description":"Grafana allows you to query, visualize, alert on and understand your metrics no matter where they are stored. Create, explore, and share dashboards with your team and foster a data driven culture.","icon":"grafana","backgroundColor":"#1857B8","available":true,"customFlags":[]},{"id":"109b8a4a-d8cf-4924-a3c5-92820369c410","label":"TTYD - Interactive Bash","description":"TTYD is a simple command-line tool for sharing terminal over the web. It is repackaged for Saagie including hadoop/hdfs, beeline, sqoop and spark (only in local) command lines.","icon":"bash","backgroundColor":"#979ba1","available":true,"customFlags":[]},{"id":"a9430810-d139-4b3b-9f05-58b974efbb6a","label":"Dash","description":"Dash empowers teams to build data science and ML apps that put the power of Python, R, and Julia in the hands of business users.","icon":"datascience","backgroundColor":"#20293d","available":true,"customFlags":[]},{"id":"4bff8629-2cc3-4f63-99bf-f2302167f58e","label":"Spark History Server","description":"The Spark history server is a monitoring tool that displays information about completed Spark applications","icon":"spark","backgroundColor":"#e25a1c","available":true,"customFlags":[]}]},{"id":"1865e9fd-adfc-46b7-a395-c3b0267e197e","name":"Nicolas C","technologies":[{}]}]}}')
        enqueueRequest('{"data":{"labWebApps":[{"id":"ba380936-613a-4301-b788-a307135cd1b7","name":"app-1"},{"id":"e9aab7ec-bab0-44be-bfc9-a9fe3efd0659","name":"testImport9"},{"id":"ad1b837b-0da7-4fcb-abd1-ba0a82539324","name":"testImport8"},{"id":"110ee233-3bb8-4693-bcb4-10c485681d43","name":"testImport7"},{"id":"6a940a01-ff10-4124-bd57-cb645659d843","name":"testImport6"},{"id":"a971c2ff-a77d-4fe0-b358-90c42fcc69d6","name":"testImport5"},{"id":"bedbcf6e-85a6-4dc5-b165-f2f3b2d22027","name":"testImport4"},{"id":"a06dbcf5-8cc9-44af-bd5f-6037ca4af250","name":"testImport3"},{"id":"db68aec6-a426-418c-8d7c-d5c7ad608a84","name":"testImport2"},{"id":"3290b0c2-8b90-4055-98e2-c12e92f3ca1e","name":"testImport"},{"id":"23430e58-e6d7-4921-91ca-bef79e7a53c1","name":"tetttttt222222"},{"id":"5a4ef2a5-ba05-43a7-974e-b4784ccadd7b","name":"testwww"}]}}')
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
        e.message.contains("Technology with name Mlflow-server is not available on the targeted server")
        e.getBuildResult().task(":${taskName}").outcome == FAILED
    }

}
