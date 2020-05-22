package io.saagie.plugin.dataops.tasks.service.exportTask

import io.saagie.plugin.dataops.DataOpsExtension
import io.saagie.plugin.dataops.utils.SaagieUtils
import io.saagie.plugin.dataops.utils.directory.FolderGenerator
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class ExportContainer {

    String exportConfigPath
    String pathWithoutFile
    String fileName
    boolean overwrite

    static final Logger logger = Logging.getLogger(ExportContainer.class)

    public ExportContainer(DataOpsExtension configuration) {

        exportConfigPath = SaagieUtils.removeLastSlash(configuration.exportArtifacts.export_file)
        pathWithoutFile = FolderGenerator.extractUrlWithoutFileName(configuration.exportArtifacts.export_file)
        fileName = FolderGenerator.extractNameFileFromUrlWithoutExtension(FolderGenerator.extractNameFileFromUrl(configuration.exportArtifacts.export_file))
        overwrite = configuration.exportArtifacts.overwrite

        File exportPath = new File(pathWithoutFile)
        logger.debug("path before: {}, ", exportConfigPath)

        File zipFolder = new File(exportConfigPath)
        if (!exportPath.exists()) {
            throw new GradleException("configuration export path does not exist")
        }

        if (overwrite && zipFolder.exists()) {
            zipFolder.delete()
        } else if (!overwrite && zipFolder.exists()) {
            throw new GradleException("Zip file already exists")
        }

        logger.debug("exportConfigPath : {}, ", exportConfigPath)

    }
}
