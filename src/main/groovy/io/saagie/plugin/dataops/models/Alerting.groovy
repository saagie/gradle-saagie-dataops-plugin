package io.saagie.plugin.dataops.models

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class Alerting implements IMapable {

    static final Logger logger = Logging.getLogger(Alerting.class)

    @Deprecated
    List<String> emails = []

    List<String> logins = []

    List<String> statusList = []

    @Override
    Map toMap() {
        if ((emails.empty && logins.empty) || statusList.empty) return null

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
