package io.saagie.plugin.dataops.utils.file

class Composite extends Component {
    private children = []
    def toString(indent) {
        def s ='';
        if (children.length > 0) {
             s = super.toString(indent, "-")
        } else {
             s = super.toString(indent)
        }
        children.each { child ->
            s += "\\n" + child.toString(indent + 4)
        }
        s
    }
    def leftShift(component) {
        children << component
    }
}
