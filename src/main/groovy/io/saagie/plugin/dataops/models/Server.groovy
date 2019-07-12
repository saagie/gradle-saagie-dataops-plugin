package io.saagie.plugin.dataops.models

class Server {
    String url
    String login
    String password
    String environment
    String proxyHost = null
    String proxyPort = null
    String acceptSelfSigned = null
    boolean jwt = false
    String realm = null
    String token = null
}
