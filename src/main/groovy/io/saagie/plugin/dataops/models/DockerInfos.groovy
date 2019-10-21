package io.saagie.plugin.dataops.models

class DockerInfos implements IMapable {
    String image
    String login
    String password

    @Override
    Map toMap() {
        if (image && login && password) {
            return [
                image: image,
                login: login,
                password: password,
            ]
        }
        return null
    }
}
