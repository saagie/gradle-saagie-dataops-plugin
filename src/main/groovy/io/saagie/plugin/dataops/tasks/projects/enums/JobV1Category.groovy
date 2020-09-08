package io.saagie.plugin.dataops.tasks.projects.enums

enum JobV1Category {
	
	dataviz(0), processing(1), extract(2)
	private int value
	
	JobV1Category( int value ) {
		this.value = value
	}
	
	public getValue() {
		return value
	}
}
