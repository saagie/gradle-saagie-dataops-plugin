package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class Alerting implements IMapable {

    @Deprecated
    List<String> emails = []

    List<String> loginEmails = []

    List<String> statusList = []

    @Override
    Map toMap() {
        if ((emails.empty && loginEmails.empty) || statusList.empty) return null

        if (emails && (!loginEmails || loginEmails.empty)) {
            return [
                emails    : emails,
                statusList: statusList
            ]
        }

        return [
            logins    : loginEmails,
            statusList: statusList
        ]
    }
}
