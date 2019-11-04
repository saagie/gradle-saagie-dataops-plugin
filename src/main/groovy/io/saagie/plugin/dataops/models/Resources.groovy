package io.saagie.plugin.dataops.models

class Resources implements IMapable {
    Float cpu
    Integer disk
    Integer memory

    Map toMap() {
        if (cpu && disk && memory) {
            return [
                cpu   : cpu,
                disk  : disk,
                memory: memory,
            ]
        }
        return null
    }
}
