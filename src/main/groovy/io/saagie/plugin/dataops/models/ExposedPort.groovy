package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExposedPort implements IMapable, IExists {
    String name
    Integer port
    Boolean isRewriteUrl
    String basePathVariableName
    Boolean isAuthenticationRequired

    @Override
    Map toMap() {
        if (!exists()) return null
        return [
            name                    : name,
            port                    : port,
            isRewriteUrl            : isRewriteUrl,
            basePathVariableName    : basePathVariableName,
            isAuthenticationRequired: isAuthenticationRequired,
        ]
    }

    @Override
    boolean exists() {
        return port
    }
}
