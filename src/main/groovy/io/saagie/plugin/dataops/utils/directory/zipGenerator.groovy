package io.saagie.plugin.dataops.utils.directory

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ZipGenerator {

    List<String> fileList
    String zipFileName
    String inputDir

    ZipGenerator(String zipFileName, String inputDir) {
        this.inputDir = inputDir
        this.zipFileName = zipFileName
        fileList = new ArrayList<String>()
    }

    void generateZip2(String zipFile) {

        byte[] buffer = new byte[1024];

        try {
            generateFileList(new File(inputDir))

            FileOutputStream fos = new FileOutputStream(zipFile)
            ZipOutputStream zos = new ZipOutputStream(fos)

            fileList.each { file ->
                ZipEntry ze = new ZipEntry(file)
                zos.putNextEntry(ze)

                FileInputStream fin = new FileInputStream(inputDir + File.separator + file)

                int len
                while ((len = fin.read(buffer)) > 0) {
                    zos.write(buffer, 0, len)
                }
                fin.close()
            }

            zos.closeEntry()
            zos.close()
        } catch (IOException ex) {
            ex.printStackTrace()
        }
    }

    void generateFileList(File node) {

        if (node.isFile()) {
            fileList.add(generateZipEntry(node.getAbsoluteFile().toString()))
        }

        if (node.isDirectory()) {
            String[] subNote = node.list()
            subNote.each { filename ->
                generateFileList(new File(node, filename))
            }
        }

    }
    String generateZipEntry(String file) {
        return file.substring(inputDir.length() + 1)
    }

    void compressDirectory(String zipFile) {
        File directory = new File(inputDir)
        getFileList(directory)

        try {
            FileOutputStream fos = new FileOutputStream(zipFile)
            ZipOutputStream zos = new ZipOutputStream(fos)

            for (String filePath : fileList) {
                System.out.println("Compressing: " + filePath)

                // Creates a zip entry.
                String name = filePath.substring(
                    directory.getAbsolutePath().length() + 1,
                    filePath.length())

                ZipEntry zipEntry = new ZipEntry(name)
                zos.putNextEntry(zipEntry)

                // Read file content and write to zip output stream.
                try {
                    FileInputStream fis = new FileInputStream(filePath)
                    byte[] buffer = new byte[2048]
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length)
                    }

                    // Close the zip entry.
                    zos.closeEntry()
                } catch (Exception e) {
                    e.printStackTrace()
                }
            }
        } catch (IOException e) {
            e.printStackTrace()
        }
    }

    /**
     * Get files list from the directory recursive to the sub directory.
     */
    void getFileList(File directory) {
        File[] files = directory.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file.getAbsolutePath());
                } else {
                    getFileList(file);
                }
            }
        }

    }
}

