package io.saagie.plugin.dataops.utils.file

abstract class Component {
    def name
    def toString(indent, specialCaractere = null) {
        if( specialCaractere) {
            ("${specialCaractere}" * indent) + name
        }
        (" " * indent) + name
    }
}
