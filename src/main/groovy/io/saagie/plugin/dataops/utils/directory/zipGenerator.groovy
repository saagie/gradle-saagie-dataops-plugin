package io.saagie.plugin.dataops.utils.directory

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipGenerator {
    def zipFileName
    def inputDir

    ZipGenerator(zipFileName, inputDir) {
        this.zipFileName = zipFileName
        this.inputDir = inputDir
    }

    def generateZipFile () {
        if (zipFileName.isEmpty()) {
            throw new Exception("zip file name is undefined")
        }
        if (inputDir.isEmpty()) {
            throw new Exception("input directory is undefined")
        }
        ZipOutputStream output = new ZipOutputStream(new FileOutputStream(zipFileName))
        new File(inputDir).eachFile() { file ->
            if (!file.canRead()) {
                throw new Exception("cannot read directory")
            }
            println file.name.toString()
            println file.toString()

            output.putNextEntry(new ZipEntry(file.name.toString())) // Create the name of the entry in the ZIP

            InputStream input = new FileInputStream(file);

            // Stream the document data to the ZIP
            Files.copy(input, output);
            output.closeEntry(); // End of current document in ZIP
            input.close()
        }
        output.close();
    }

    def generateZip2() {
        ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(zipFileName))
        new File(inputDir).eachFile() { file ->
            //check if file
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
}
