package io.saagie.plugin.dataops.utils


import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging;

class ZipUtils {
    static final Logger logger = Logging.getLogger(ZipUtils.class)
    /**
     * Zip provided files
     * @param inputDir Directory to zip
     * @param zipFileName Name of the zipFile to create
     * @return {File} zipPath File object where the zip file is created
     */

    static File unzip(String zipFileName, String outputDir) {
        try {
            ZipFile zipFile = new ZipFile(zipFileName);
            zipFile.extractAll(outputDir);
        } catch (ZipException e) {
            logger.error("unzipping failed");
            logger.error(e.message)
            e.printStackTrace();
        }

    }
}
