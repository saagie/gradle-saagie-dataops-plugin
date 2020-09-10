package io.saagie.plugin.dataops.clients

import spock.lang.Shared
import spock.lang.Specification

class SaagieClientUtilsTest extends Specification {
    @Shared
    ClassLoader classLoader = getClass().getClassLoader()
    @Shared
    String exportJobZipFilename = 'exportedJob.zip'

    def "expect that the testExportJob.zip test file exists in the resource folder"() {
        when:
        URL resource = classLoader.getResource(exportJobZipFilename)
        File unzippedExportedConfig = new File(resource.getFile())

        then:
        notThrown(Exception)
        unzippedExportedConfig.exists()
    }
}
