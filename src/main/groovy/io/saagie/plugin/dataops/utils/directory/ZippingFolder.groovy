package io.saagie.plugin.dataops.utils.directory

import org.gradle.api.GradleException

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ZippingFolder {
    List fileList = new ArrayList();
    String inputDir
    String zipFileName

    ZippingFolder (String zipFileName, String inputDir) {
        this.inputDir = inputDir
        this.zipFileName = zipFileName
    }

    void generateZip( String zipDir){
        final int BUFFER = 2048
        BufferedInputStream bis = null
        ZipOutputStream zos = null
        try{
            FileOutputStream fos = new FileOutputStream(zipDir)
            zos = new ZipOutputStream(fos)
            fileList.each{ file ->
                if(file.isDirectory()){
                    ZipEntry ze = new ZipEntry(file.getName()+"/")
                    zos.putNextEntry(ze);
                    zos.closeEntry();
                }
                else{
                    FileInputStream fis = new FileInputStream(file)
                    bis = new BufferedInputStream(fis, BUFFER)
                    ZipEntry ze = new ZipEntry(getFileName(inputDir, file.toString()))
                    zos.putNextEntry(ze)
                    byte[] data = new byte[BUFFER]
                    int count;
                    while((count = bis.read(data, 0, BUFFER)) != -1) {
                        zos.write(data, 0, count)
                    }
                    bis.close()
                    zos.closeEntry()
                }
            }

        }catch(IOException ioExp){
            throw new GradleException("something went wrong when zipping the folder")
        } finally{
            try {
                zos.close()
                if(bis != null)
                    bis.close()
            } catch (IOException e) {
                throw new GradleException("something went wrong when closing the zipping folder")
            }
        }
    }
    void getListOfFiles(File source){
        File[] fileNames = source.listFiles()
        fileNames.each{ file->
            if(file.isDirectory()){
                fileList.add(file)
                getListOfFiles(file)
            }else{
                fileList.add(file)
            }
        }
    }

    String getFileName(String ROOT_DIR, String filePath){
        String name = filePath.substring(ROOT_DIR.length() + 1, filePath.length())
        return name
    }
}
