package io.saagie.plugin.dataops.models

class App extends Job {
    int storageSizeInMB;
    Boolean isStreaming;

    @Override
    Map toMap() {
        return [*              : super.toMap(),
                storageSizeInMB: storageSizeInMB,
                isStreaming : isStreaming
        ]
    }
}
