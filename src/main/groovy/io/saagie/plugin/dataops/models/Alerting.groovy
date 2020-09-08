package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class Alerting implements IMapable {

    @Deprecated
    List<String> emails = []

    List<String> logins = []

    List<String> statusList = []

    @Override
    Map toMap() {
        if ((emails && emails.empty && logins && logins.empty) || (statusList && statusList.empty))
            return null

        if (emails && (!logins || logins.empty)) {
            return [
                    emails    : emails,
                    statusList: statusList
            ]
        }

        return [
                logins    : logins,
                statusList: statusList
        ]
    }
}
