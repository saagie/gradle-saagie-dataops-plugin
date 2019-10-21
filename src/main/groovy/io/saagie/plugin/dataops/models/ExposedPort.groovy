package io.saagie.plugin.dataops.models

class ExposedPort implements IMapable {
    String name
    Integer port
    Boolean isRewriteUrl
    String basePathVariableName
    Boolean isAuthenticationRequired

    @Override
    Map toMap() {
        if (!port) return null
        return [
            name                    : name,
            port                    : port,
            isRewriteUrl            : isRewriteUrl,
            basePathVariableName    : basePathVariableName,
            isAuthenticationRequired: isAuthenticationRequired,
        ]
    }
}
