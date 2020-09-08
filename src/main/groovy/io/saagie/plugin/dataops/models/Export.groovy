package io.saagie.plugin.dataops.models

import javax.validation.constraints.NotBlank

class Export {

    @NotBlank(message = 'export path cannot be empty')
    String export_file
    /**
     * Force overwrite the export zip to the path
     */

    String temporary_directory

    boolean overwrite = false

}
