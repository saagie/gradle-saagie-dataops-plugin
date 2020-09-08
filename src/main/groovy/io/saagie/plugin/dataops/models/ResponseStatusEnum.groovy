package io.saagie.plugin.dataops.models

enum ResponseStatusEnum {
	success('success'),
	failure('failure') ;
	
	public final String name ;
	
	private ResponseStatusEnum( String s ) {
		name = s ;
	}
	
	public boolean equalsStatus( String otherStatus ) {
		return name.equals(otherStatus) ;
	}
	
	public String toString() {
		return this.name ;
	}
}
