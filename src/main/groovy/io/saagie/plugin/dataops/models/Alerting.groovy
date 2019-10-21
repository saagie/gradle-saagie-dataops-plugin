package io.saagie.plugin.dataops.models

class Alerting implements IMapable {
    List<String> emails = []
    List<String> statusList = []

    @Override
    Map toMap() {
        if (emails.empty || statusList.empty) {
            return null
        }
        return [
            emails: emails,
            statusList: statusList
        ]
    }
}
