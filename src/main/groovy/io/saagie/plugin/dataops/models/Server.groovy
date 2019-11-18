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

    String acceptSelfSigned = null

    boolean jwt = false

    String realm = null

    String token = null
}
