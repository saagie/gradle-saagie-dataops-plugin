package io.saagie.plugin.dataops.models

class Alerting implements IMapable {
    List<String> emails = []
    List<String> logins = []
    List<String> statusList = []

    @Override
    Map toMap() {
        if (emails.empty || logins.empty || statusList.empty) return null
        return [
            emails    : emails,
            logins    : logins,
            statusList: statusList
        ]
    }
}
