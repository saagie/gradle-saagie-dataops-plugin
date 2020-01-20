package io.saagie.plugin.dataops.models

import javax.validation.constraints.NotBlank

class Export {

    @NotBlank(message = 'export path cannot be empty')
    String export_file_path
    /**
     * Force overwrite the export zip to the path
     */
    boolean overwrite = false

}
