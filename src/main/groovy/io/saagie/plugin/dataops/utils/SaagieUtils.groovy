package io.saagie.plugin.dataops.utils

import io.saagie.plugin.dataops.DataOpsExtension
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody

class SaagieUtils {
    DataOpsExtension configuration

    SaagieUtils(DataOpsExtension configuration) {
        this.configuration = configuration
    }

    static String gq(String request) {
        def inlinedRequest = request.replaceAll('\\n', '')
        """{ "query": "$inlinedRequest" }"""
    }

    static final MediaType JSON = MediaType.parse("application/json; charset=utf-8")

    Request getProjectsRequest() {
        def listProjectsRequest = gq('''
            {
                projects {
                    id
                    name
                    creator
                    description
                    jobsCount
                    status
                }
            }
        ''')

        RequestBody body = RequestBody.create(JSON, listProjectsRequest);
        def request = new Request.Builder()
            .url("${configuration.server.url}/api/v1/projects/platform/${configuration.server.environment}/graphql")
            .addHeader(
                'Authorization',
                Credentials.basic(configuration.server.login, configuration.server.password)
            )
            .post(body)
            .build();
        return request
    }
}
