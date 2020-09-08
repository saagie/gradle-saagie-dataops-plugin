package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class PackageInfo implements IMapable, IExists {
	String name
	String downloadUrl ;
	
	@Override
	Map toMap() {
		if (exists()) {
			return [
					name        : name,
					downloadUrl : downloadUrl]
		}
		return null
	}
	
	@Override
	boolean exists() {
		return name || downloadUrl
	}
	
	@Override
	boolean equals( o ) {
		if (this.is(o)) return true
		if (getClass() != o.class) return false
		
		PackageInfo packageInfo = (PackageInfo) o
		
		if (name != packageInfo.name) return false
		
		return true
	}
}
