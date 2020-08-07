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
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.ssl.SSLContexts
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

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
	
	private HttpClientBuilder() {}
	
	static OkHttpClient getHttpClient( DataOpsExtension configuration ) {
		Server server = configuration.server
		OkHttpClient client = new OkHttpClient()
		if ( server.acceptSelfSigned ) {
			TrustManager[] trustAllCerts = [
					new X509TrustManager() {
						@Override
						void checkClientTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
						}
						
						@Override
						void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
						}
						
						@Override
						X509Certificate[] getAcceptedIssuers() {
							return [ ] as X509Certificate[]
						}
					}
			] as TrustManager[]
			
			SSLContext trustAllSslContext = SSLContext.getInstance( "SSL" )
			trustAllSslContext.init( null, trustAllCerts, new SecureRandom() )
			SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory()
			def clientBuilder = client.newBuilder()
			clientBuilder.connectTimeout( configuration.server.timeout, TimeUnit.SECONDS )
			clientBuilder.readTimeout( configuration.server.timeout, TimeUnit.SECONDS )
			clientBuilder.writeTimeout( configuration.server.timeout, TimeUnit.SECONDS )
			client = clientBuilder.sslSocketFactory( trustAllSslSocketFactory, ( X509TrustManager ) trustAllCerts[ 0 ] )
					.hostnameVerifier( new HostnameVerifier() {
						@Override
						boolean verify( String hostname, SSLSession session ) {
							return true
						}
					} )
					.build()
		}
		return client
	}
	
	static OkHttpClient getHttpClientV1( DataOpsExtension configuration ) {
		OkHttpClient.Builder builder = new OkHttpClient.Builder()
		if ( configuration.server.acceptSelfSigned ) {
			SSLContext sslContext = SSLContexts.custom().loadTrustMaterial( null, new TrustSelfSignedStrategy() {
				@Override
				boolean isTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
					true
				}
			} ).build()
			builder.sslSocketFactory( sslContext.getSocketFactory(),
					new X509TrustManager() {
						@Override
						void checkClientTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
						}
						
						@Override
						void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
						}
						
						@Override
						X509Certificate[] getAcceptedIssuers() {
							return new X509Certificate[0]
						}
					} )
		}
		
		builder.authenticator( new Authenticator() {
			@Override
			Request authenticate( Route route, Response response ) throws IOException {
				return response.request().newBuilder().header( 'Authorization', Credentials.basic( configuration.server.login, configuration.server.password ) ).build()
			}
		} )
		builder.connectTimeout( 2, TimeUnit.SECONDS )
		builder.readTimeout( 20, TimeUnit.SECONDS )
		builder.writeTimeout( 8, TimeUnit.SECONDS )
		builder.build()
	}
	
}
