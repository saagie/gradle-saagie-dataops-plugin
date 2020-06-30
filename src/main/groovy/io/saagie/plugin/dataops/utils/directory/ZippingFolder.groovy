package io.saagie.plugin.dataops.utils.directory

import net.lingala.zip4j.ZipFile
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class ZippingFolder {
    static final Logger logger = Logging.getLogger(ZippingFolder.class)
    String inputDir
    String zipFileName
    Boolean isNotDefaultTemp

    ZippingFolder (String zipFileName, String inputDir, isNotDefaultTemp) {
        this.inputDir = inputDir
        this.zipFileName = zipFileName
        this.isNotDefaultTemp = isNotDefaultTemp
    }

    void generateZip( File tempFile ) throws IOException {
        try{
            new ZipFile( zipFileName).addFolder( new File( inputDir))
            logger.debug("Temporary file cache name : ${tempFile.name}")
            if(isNotDefaultTemp){
                deleteInsideDirectory( new File( tempFile.getParent()))
            } else {
                boolean isDeletedTmpFile = tempFile.deleteDir()
                if(!isDeletedTmpFile) {
                    throw new GradleException("One of the files didn't get deleted. File name: ${tempFile.name}")
                }
            }
        } catch (IOException ex) {
            throw new GradleException(ex.message)
        }
    }

    static deleteInsideDirectory(File folder) {
        String [] entries = folder.list()

        for (String s: entries) {
            Boolean isDeleted = true
            File currentFile = new File( folder.getPath(), s)
            isDeleted = currentFile.deleteDir()
            if (!isDeleted) {
                throw new GradleException("One of the files didn't get deleted. File name: ${s}. Path name: ${folder.getPath()}")
            } else {
                logger.debug("Deleted custom tmp file : ${s} in path : ${folder.getPath()}")
            }
        }
    }
}
