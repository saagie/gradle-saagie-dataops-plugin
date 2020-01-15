package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExportJob implements IExists{
    def job
    def jobVersion
    String downloadUrl

    @Override
    boolean exists() {
        return job && jobVersion && downloadUrl
    }
}
