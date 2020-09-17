package io.saagie.plugin.dataops.utils

import groovy.transform.TypeChecked
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Server
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.ssl.SSLContexts

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

@TypeChecked
class HttpClientBuilder {

    static private X509TrustManager trustManagerInstance = new X509TrustManager() {
        @Override
        void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        X509Certificate[] getAcceptedIssuers() {
            return [] as X509Certificate[]
        }
    };
    private HttpClientBuilder() {}
    static OkHttpClient getHttpClient(DataOpsExtension configuration) {
        Server server = configuration.server
        OkHttpClient client = new OkHttpClient()

        if (server.acceptSelfSigned) {
            TrustManager[] trustAllCerts = [trustManagerInstance] as TrustManager[]

            SSLContext trustAllSslContext = SSLContext.getInstance("SSL")
            trustAllSslContext.init(null, trustAllCerts, new SecureRandom())
            SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory()
            def clientBuilder = client.newBuilder()
            client = setTimeOutForBuildFromTheConfiguration(clientBuilder, configuration).sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    boolean verify(String hostname, SSLSession session) {
                        return true
                    }
                })
                .build()
        }
        return client
    }

    static OkHttpClient getHttpClientV1(DataOpsExtension configuration) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
        if (configuration.server.acceptSelfSigned) {
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy() {
                @Override
                boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    true
                }
            }).build()
            builder.sslSocketFactory(sslContext.getSocketFactory(),trustManagerInstance)
        }

        builder.authenticator(new Authenticator() {
            @Override
            Request authenticate(Route route, Response response) throws IOException {
                return response.request().newBuilder().header('Authorization', Credentials.basic(configuration.server.login, configuration.server.password)).build()
            }
        })
        setTimeOutForBuildFromTheConfiguration(builder, configuration).build()
    }

    static setTimeOutForBuildFromTheConfiguration(OkHttpClient.Builder builder, DataOpsExtension configuration) {
        builder.connectTimeout(configuration.server.timeout, TimeUnit.SECONDS)
        builder.readTimeout(configuration.server.timeout, TimeUnit.SECONDS)
        builder.writeTimeout(configuration.server.timeout, TimeUnit.SECONDS)
        return builder;
    }

}
