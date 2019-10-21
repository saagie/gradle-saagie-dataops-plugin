package io.saagie.plugin.dataops.models

class PackageInfo implements IMapable {
    String name

    @Override
    Map toMap() {
        if (name) {
            return [name: name]
        }
        return null
    }
}
