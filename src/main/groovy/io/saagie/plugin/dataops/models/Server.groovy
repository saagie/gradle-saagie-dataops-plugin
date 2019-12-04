package io.saagie.plugin.dataops.models

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

class Server {

    @NotBlank(message = 'url cannot be empty')
    String url

    @NotBlank(message = 'login cannot be empty')
    String login

    @NotNull(message = 'password cannot be null')
    String password

    String environment

    // TODO: implement these options
    String proxyHost = null

    String proxyPort = null

    /**
     * Force the plugin to disable
     * SSL validation
     */
    String acceptSelfSigned = null

    /**
     * Force the plugin to login with a cookie,
     * and not with basic auth.
     */
    boolean jwt = false

    /**
     * Force the plugin to use the old upload api
     * Impacted tasks are those which uses a file upload
     */
    boolean useLegacy = false

    /**
     * You should not use this param;
     * it is computed from the url.
     * Only needed when the `jwt` option
     * is set.
     */
    String realm = null

    /**
     * Used internaly to store the generated
     * token from `login` / `password`
     */
    String token = null
}
