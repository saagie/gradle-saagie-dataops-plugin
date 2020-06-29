package io.saagie.plugin.dataops.utils

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ZipUtils {

    /**
     * Zip provided files
     * @param inputDir Directory to zip
     * @param zipFileName Name of the zipFile to create
     * @return {File} zipPath File object where the zip file is created
     */

    static File zip(String inputDir, String zipFileName) {
        ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(zipFileName))
        new File(inputDir).eachFile() { file ->
            if (file.isFile()){
                zipFile.putNextEntry(new ZipEntry(file.name))
                def buffer = new byte[file.size()]
                file.withInputStream {
                    zipFile.write(buffer, 0, it.read(buffer))
                }
                zipFile.closeEntry()
            }
        }
        zipFile.close()
    }

    static File unzip(String zipFileName, String outputDir) {
        def zip = new ZipFile(new File(zipFileName))
        zip.entries().each{
            if (!it.isDirectory()) {
                def fOut = new File(outputDir + File.separator + it.name)
                new File(fOut.parent).mkdirs()
                def fos = new FileOutputStream(fOut)
                def buf = new byte[it.size]
                def len = zip.getInputStream(it).read(buf)
                fos.write(buf, 0, len)
                fos.close()
            }
        }
        zip.close()
    }
}
