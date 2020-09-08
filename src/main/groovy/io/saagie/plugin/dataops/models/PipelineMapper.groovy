package io.saagie.plugin.dataops.models

class PipelineMapper {
	Pipeline pipeline = new Pipeline()
	PipelineVersion pipelineVersion = new PipelineVersion()
	
	Object pipeline( Closure closure ) {
		pipeline.with(closure)
	}
	
	Object pipelineVersion( Closure closure ) {
		pipelineVersion.with(closure)
	}
}
