package io.saagie.plugin.dataops.models

class EnvVariable {

    String scope = EnvVarScopeTypeEnum.project.name()

    ArrayList<String> name

    boolean include_all_var = false

}
