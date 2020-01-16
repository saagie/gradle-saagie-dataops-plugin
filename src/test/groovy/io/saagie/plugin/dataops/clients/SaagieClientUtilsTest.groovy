package io.saagie.plugin.dataops.clients

import spock.lang.Shared
import spock.lang.Specification

class SaagieClientUtilsTest extends Specification {
    @Shared ClassLoader classLoader = getClass().getClassLoader()
    @Shared String exportJobZipFilename = 'exportedJob'

    def "expect that the testExportJob.zip test file exists in the resource folder"() {
        when:
        URL resource = classLoader.getResource(exportJobZipFilename)
        File unzippedExportedConfig = new File(resource.toURI())

        then:
        notThrown(Exception)
        unzippedExportedConfig.exists()
    }

    def "expect that the extractJobConfigAndPackageFromExportedZip return config from a zip file"() {
        given:
        URL resource = classLoader.getResource(exportJobZipFilename)
        File unzippedExportedConfig = new File(resource.toURI())

        when:
        Map exportedConfig = SaagieClientUtils.extractJobConfigAndPackageFromExportedJob(unzippedExportedConfig)

        then:
        notThrown(Exception)
        exportedConfig.jobs['d936c1d5-86e9-4268-b65a-82e17b344046'] != null
        exportedConfig.jobs['d936c1d5-86e9-4268-b65a-82e17b344046'].configOverride != null
        exportedConfig.jobs['d936c1d5-86e9-4268-b65a-82e17b344046'].package.exists()
    }
}
