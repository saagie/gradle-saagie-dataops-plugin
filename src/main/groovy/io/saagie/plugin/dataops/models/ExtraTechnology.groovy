package io.saagie.plugin.dataops.models

class ExtraTechnology implements IMapable {
    String language
    String version

    @Override
    Map toMap() {
        if (language && version) {
            return [
                language: language,
                version : version
            ]
        }
        return null
    }
}
