package io.saagie.plugin.dataops.models

class PropertyOverride implements IExists{
	String scope;
	
	@Override
	boolean exists() {
		return !scope?.isEmpty()
	}
}
