package io.saagie.plugin.dataops.models

class PipelineDTO implements IExists {
	String name
	String id
	String description
	Boolean isScheduled
	String cronScheduling
	AlertingDTO alerting = new AlertingDTO()
	
	Object alerting( Closure closure ) {
		alerting.with(closure)
	}
	
	@Override
	boolean exists() {
		return name ||
				id ||
				description ||
				alerting ||
				isScheduled ||
				cronScheduling
	}
	
	void setPipelineFromApiResult( pipelineDetailResult ) {
		name = pipelineDetailResult.name
		id = pipelineDetailResult.id
		description = pipelineDetailResult.description
		isScheduled = pipelineDetailResult.isScheduled
		cronScheduling = pipelineDetailResult.cronScheduling
		alerting = pipelineDetailResult.alerting
		
	}
	
	void setPipelineFromV1ApiResult( pipelineV1DetailResult ) {
		name = pipelineV1DetailResult.name
		id = pipelineV1DetailResult.id
		isScheduled = pipelineV1DetailResult.hasProperty("schedule") ? true : false
		if (isScheduled) {
			cronScheduling = pipelineV1DetailResult.schedule
		}
	}
}
