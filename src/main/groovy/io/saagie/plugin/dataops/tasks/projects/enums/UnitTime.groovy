package io.saagie.plugin.dataops.tasks.projects.enums

enum UnitTime {
    SECOND(0), MINUTE(1), HOUR(2), DAYOFMONTH(3), MONTH(4), YEAR(5)

    UnitTime(int value) {
        this.value = value
    }
    private final int value

    int getValue() {
        value
    }
}
