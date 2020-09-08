package io.saagie.plugin.dataops.models

public enum EnvVarScopeTypeEnum {
    global('global'),
    project('project');

    private final String name;

    private EnvVarScopeTypeEnum(String s) {
        name = s;
    }

    public boolean equalsName(String otherName) {
        // (otherName == null) check is not needed because name.equals(null) returns false
        return name.equals(otherName);
    }

    public String toString() {
        return this.name;
    }
}
