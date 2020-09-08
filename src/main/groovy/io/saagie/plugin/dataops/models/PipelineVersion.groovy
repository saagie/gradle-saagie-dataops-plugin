package io.saagie.plugin.dataops.models

class PipelineVersion implements IMapable {
    String releaseNote
    List<String> jobs = []

    @Override
    Map toMap() {
        if (releaseNote && !jobs.empty) {
            return [
                    releaseNote: releaseNote,
                    jobs       : jobs
            ]
        }
        return null
    }
}
