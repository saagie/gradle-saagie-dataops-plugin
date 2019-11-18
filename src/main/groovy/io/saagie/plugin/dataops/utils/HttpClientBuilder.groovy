package io.saagie.plugin.dataops.utils

import groovy.transform.TypeChecked
import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.models.Server
import okhttp3.OkHttpClient

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

@TypeChecked
class HttpClientBuilder {

    private HttpClientBuilder() {}

    static OkHttpClient getHttpClient(DataOpsExtension configuration) {
        Server server = configuration.server
        OkHttpClient client = new OkHttpClient()

        if (server.acceptSelfSigned) {
            TrustManager[] trustAllCerts = [
                new X509TrustManager() {
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
                }
            ] as TrustManager[]

            SSLContext trustAllSslContext  = SSLContext.getInstance("SSL")
            trustAllSslContext.init(null, trustAllCerts, new SecureRandom())
            SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory()
            client = client.newBuilder()
                .sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager) trustAllCerts[0])
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
}
