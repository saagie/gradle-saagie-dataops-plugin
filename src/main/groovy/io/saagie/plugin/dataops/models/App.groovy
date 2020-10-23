package io.saagie.plugin.dataops.models

class App extends Job {
    int storageSizeInMB;
    @Override
    Map toMap() {
        return [*              : super.toMap(),
                storageSizeInMB: storageSizeInMB
        ]
    }
}
