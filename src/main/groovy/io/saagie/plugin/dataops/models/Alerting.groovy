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

        if (emails) {
            logger.warn('[Deprecation warning] You should use the field logins instead of emails')
            logger.warn('[Deprecation warning] Using logins with the content of emails')
            logins = emails
        }

        return [
            logins    : logins,
            statusList: statusList
        ]
    }
}
