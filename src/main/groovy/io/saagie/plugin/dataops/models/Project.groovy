package io.saagie.plugin.dataops.models

class Project implements IMapable {
    String id
    String name
    String creator
    String description
    Integer jobsCount
    String status

    @Override
    Map toMap() {
        if (id && name && creator && description && jobsCount && status) {
            return [
                id: id,
                name: name,
                creator: creator,
                description: description,
                jobsCount: jobsCount,
                status: status
            ]
        }
        return null
    }
}
