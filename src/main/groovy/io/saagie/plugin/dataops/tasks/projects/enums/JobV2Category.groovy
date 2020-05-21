package io.saagie.plugin.dataops.tasks.projects.enums

enum JobV2Category {
    SMART_APP(0, "Smart App"), PROCESSING(1, "Processing"), EXTRACTION(2, "Extraction")

    JobV2Category(Integer value, String name) {
        this.value = value
        this.name = name
    }

    private final Integer value
    private final String name

    Integer getValue() {
        value
    }

    String getName(){
        name
    }

    static def getPerValue(Integer value) {
        def test = null;
        values().each {
            if(it.value == value) {
                test = it.name
            }
        }
        return test
    }
}
