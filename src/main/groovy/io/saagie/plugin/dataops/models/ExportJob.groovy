package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExportJob {
    Job job
    JobVersion jobVersion
    String DownloadUrl

    @Override
    boolean exists() {
        return job && jobVersion && DownloadUrl
    }
}
