package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked
import org.jetbrains.annotations.NotNull

@TypeChecked
class ExtraTechnology implements IMapable, IExists {
	String language
	String version
	
	@Override
	Map toMap() {
		if (exists()) {
			return [
					language : language,
					version  : version
			]
		}
		return null
	}
	
	@Override
	boolean exists() {
		return language && version
	}
	
	@Override
	boolean equals( o ) {
		println "equals(o) triggered"
		if (this.is(o)) return true
		if (getClass() != o.class) return false
		
		ExtraTechnology extraTechnology = (ExtraTechnology) o
		
		if (language != extraTechnology.language) return false
		if (version != extraTechnology.version) return false
		
		return true
	}
}
