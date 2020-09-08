package io.saagie.plugin.dataops.models

class PipelineInstance implements IMapable {
	String id
	
	@Override
	Map toMap() {
		if (id) return [id : id]
		return null
	}
}
