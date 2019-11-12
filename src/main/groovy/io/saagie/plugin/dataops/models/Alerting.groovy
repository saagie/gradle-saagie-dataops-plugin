package io.saagie.plugin.dataops.models

class Alerting implements IMapable {
    @Deprecated
    List<String> emails = []
    List<String> logins = []
    List<String> statusList = []

    @Override
    Map toMap() {
        if (emails.empty || logins.empty || statusList.empty) return null

        if (emails) {
            logins = emails
        }

        return [
            logins    : logins,
            statusList: statusList
        ]
    }
}
