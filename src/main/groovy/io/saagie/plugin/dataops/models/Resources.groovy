package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class Resources implements IMapable, IExists {
	Float cpu
	Integer disk
	Integer memory
	
	Map toMap() {
		if (exists()) {
			return [
					cpu    : cpu,
					disk   : disk,
					memory : memory,
			]
		}
		return null
	}
	
	@Override
	boolean exists() {
		return cpu && disk && memory
	}
}
