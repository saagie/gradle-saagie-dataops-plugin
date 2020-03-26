package io.saagie.plugin.dataops.utils.directory

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    void generateZip(File tempFile) throws IOException{
        try{
            FileOutputStream fos = new FileOutputStream(zipFileName)
            ZipOutputStream zipOut = new ZipOutputStream(fos)
            File fileToZip = new File(inputDir)
            zipFile(fileToZip, fileToZip.getName(), zipOut)
            zipOut.close()
            fos.close()
            if(isNotDefaultTemp){
                deleteInsideDirectory(tempFile)
            } else {
                tempFile.deleteDir()
            }
        } catch (IOException ex) {
            throw new GradleException(ex.message)
        }
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName))
                zipOut.closeEntry()
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"))
                zipOut.closeEntry()
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip)
        ZipEntry zipEntry = new ZipEntry(fileName)
        zipOut.putNextEntry(zipEntry)
        byte[] bytes = new byte[1024];
        int length
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length)
        }
        fis.close()
    }

    static deleteInsideDirectory(File folder) {
        String[]entries = folder.list();
        for(String s: entries){
            File currentFile = new File(folder.getPath(),s);
            currentFile.deleteDir();
        }
    }
}
