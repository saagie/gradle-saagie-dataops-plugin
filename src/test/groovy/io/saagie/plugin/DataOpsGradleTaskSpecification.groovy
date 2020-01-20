package io.saagie.plugin

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

import java.nio.ByteBuffer

/**
 * Class used to create basic functions and attributes
 * in order to be able to test gradle tasks.
 *
 * Your test class must extend this class
 */
class DataOpsGradleTaskSpecification extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    @Shared MockWebServer mockWebServer = new MockWebServer()

    File buildFile
    File jobFile

    def setupSpec() {
        mockWebServer.start(9000)
    }

    def cleanupSpec() {
        mockWebServer.shutdown()
    }

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << 'plugins { id "io.saagie.gradle-saagie-dataops-plugin" }\n'

        jobFile = testProjectDir.newFile('jobFile.py')
    }

    def cleanup() {
        mockWebServer.dispatcher.peek()
    }

    BuildResult gradle(boolean isSuccessExpected, String[] arguments = ['tasks']) {
        arguments += '--stacktrace'
        def runner = GradleRunner.create()
            .withArguments(arguments)
            .withProjectDir(testProjectDir.root)
            .withPluginClasspath()
            .withDebug(true)

        return isSuccessExpected ? runner.build() : runner.buildAndFail();
    }

    BuildResult gradle(String[] arguments = ['tasks']) {
        gradle(true, arguments)
    }

    def enqueueRequest(String body, status = 200) {
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = status
        mockedResponse.body = body
        mockWebServer.enqueue(mockedResponse)
    }

    def enqueueRequestFile(File body, status = 200) {
        def mockedResponse = new MockResponse()
        mockedResponse.responseCode = status
        mockedResponse.addHeader("Content-Type: application/octet-stream")
        mockedResponse.addHeader("Content-Disposition: attachement;filename=\"" + body.name + "\"")
        mockedResponse.body = this.getBinaryFileAsBuffer(body)
        mockWebServer.enqueue(mockedResponse)
    }
    Buffer getBinaryFileAsBuffer(File file) throws IOException {
        byte[] fileData = FileUtils.readFileToByteArray(file)
        Buffer buf = new Buffer()
        buf.write(fileData)
        return buf
    }
}
