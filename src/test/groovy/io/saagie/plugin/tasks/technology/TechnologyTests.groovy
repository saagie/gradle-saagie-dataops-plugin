package io.saagie.plugin.tasks.technology

import io.saagie.plugin.DataOpsGradleTaskSpecification
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.BuildResult
import spock.lang.Shared
import spock.lang.Title

import static io.saagie.plugin.dataops.DataOpsModule.PROJECTS_LIST_TECHNOLOGIES_TASK

@Title("projectsListTechnologies task tests")
class TechnologyTests extends DataOpsGradleTaskSpecification {
    @Shared
    String taskName = PROJECTS_LIST_TECHNOLOGIES_TASK

    final DATA_RESPONSE = """
                    {
                      "data": {
                        "technologiesByIds": [
                          {
                            "id": "1d117fb6-0697-438a-b419-a69e0e7406e8",
                            "label": "Spark",
                            "available": true,
                            "contexts": [
                              {
                                "job": {
                                  "features": []
                                }
                              },
                              {
                                "job": {
                                  "features": []
                                }
                              },
                              {
                                "job": {
                                  "features": []
                                }
                              }
                            ]
                          },
                          {
                            "id": "a7f89b17-e078-46ab-ba48-be4fae8dd606",
                            "label": "Bash",
                            "available": true,
                            "contexts": [
                              {
                                "job": {
                                  "features": [
                                    {
                                      "type": "COMMAND_LINE",
                                      "label": "Command line",
                                      "mandatory": true,
                                      "comment": "Linux shell command",
                                      "defaultValue": "echo \\"Saagie Bash\\""
                                    },
                                    {
                                      "type": "ARTIFACT",
                                      "label": "Package",
                                      "mandatory": true,
                                      "comment": "All files are accepted.",
                                      "defaultValue": null
                                    },
                                    {
                                      "type": "SCHEDULER",
                                      "label": "Scheduled",
                                      "mandatory": false,
                                      "comment": null,
                                      "defaultValue": null
                                    }
                                  ]
                                }
                              },
                              {
                                "job": {
                                  "features": [
                                    {
                                      "type": "COMMAND_LINE",
                                      "label": "Command line",
                                      "mandatory": true,
                                      "comment": "Linux shell command",
                                      "defaultValue": "# To configure AWS credential don't forget to set:\\n# \\tAWS_ACCESS_KEY_ID\\n# \\tAWS_SECRET_ACCESS_KEY\\n# For other ways to log into aws please refer to aws documentation.\\n# If done right you can issue your AWS commands as usual e.g. :\\n\\naws s3 ls"
                                    },
                                    {
                                      "type": "ARTIFACT",
                                      "label": "Package",
                                      "mandatory": true,
                                      "comment": "All files are accepted.",
                                      "defaultValue": null
                                    },
                                    {
                                      "type": "SCHEDULER",
                                      "label": "Scheduled",
                                      "mandatory": false,
                                      "comment": null,
                                      "defaultValue": null
                                    }
                                  ]
                                }
                              }
                            ]
                          }
                        ]
                      }
                    }
                    """

    def "projectsListTechnologies task should list technologies of a project"() {
        given:
        def mockedTechnoList = new MockResponse()
        mockedTechnoList.responseCode = 200
        mockedTechnoList.body="""
        {
          "data": {
            "project": {
              "technologiesByCategory": [
                {
                  "technologies": [
                    {
                      "id": "1d117fb6-0697-438a-b419-a69e0e7406e8"
                    },
                    {
                      "id": "a7f89b17-e078-46ab-ba48-be4fae8dd606"
                    }
                  ]
                },
                {
                  "technologies": []
                },
                {
                  "technologies": []
                }
              ]
            }
          }
        }
        """
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = DATA_RESPONSE

        buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }

                project {
                    id = 'dezdezjiodjei-892a-2342-8552-5be4b6de5df4'
                }
            }
        """
        mockWebServer.enqueue(mockedTechnoList)
        mockWebServer.enqueue(mockedResponse)

        when:
        BuildResult result = gradle(taskName)

        then:
        !result.output.contains('"data"')
        result.output.contains('"label"')
        result.output.contains('"features"')
    }

    def "listed technologies should not show duplicated entries"() {
        given:
        def mockedTechnoList = new MockResponse()
        mockedTechnoList.responseCode = 200
        mockedTechnoList.body="""
        {
          "data": {
            "project": {
              "technologiesByCategory": [
                {
                  "technologies": [
                    {
                      "id": "1d117fb6-0697-438a-b419-a69e0e7406e8"
                    },
                    {
                      "id": "1d117fb6-0697-438a-b419-a69e0e7406e8"
                    }
                  ]
                },
                {
                  "technologies": [
                    {
                      "id": "a7f89b17-e078-46ab-ba48-be4fae8dd606"
                    }
                  ]
                },
                {
                  "technologies": []
                }
              ]
            }
          }
        }
        """
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = 200
        mockedResponse.body = DATA_RESPONSE
            buildFile << """
            saagie {
                server {
                    url = 'http://localhost:9000'
                    login = 'fake.user'
                    password = 'ThisPasswordIsWrong'
                    environment = 2
                }

                project {
                    id = 'dezdezjiodjei-892a-2342-8552-5be4b6de5df4'
                }
            }
        """
        mockWebServer.enqueue(mockedTechnoList)
        mockWebServer.enqueue(mockedResponse)

        when:
        BuildResult result = gradle(taskName)

        then:
        result.output.count('1d117fb6-0697-438a-b419-a69e0e7406e8') == 1
        result.output.count('a7f89b17-e078-46ab-ba48-be4fae8dd606') == 1
    }
}
