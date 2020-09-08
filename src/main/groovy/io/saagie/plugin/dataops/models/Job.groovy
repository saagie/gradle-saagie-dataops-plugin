package io.saagie.plugin.dataops.models

class Job implements IMapable {
	String name
	String id
	def ids = []
	String projectId
	String description
	String category
	String technology
	Boolean include_all_versions
	Boolean isScheduled = false
	Boolean isStreaming = false
	String cronScheduling
	Alerting alerting = new Alerting()
	
	Object alerting( Closure closure ) {
		alerting.with(closure)
	}
	
	@Override
	Map toMap() {
		if (technology) {
			return [
					name           : name,
					id             : id,
					projectId      : projectId,
					description    : description,
					category       : category,
					technology     : [id : technology],
					cronScheduling : cronScheduling,
					isScheduled    : isScheduled,
					isStreaming    : isStreaming,
					alerting       : alerting.toMap(),
			]
		} else {
			return [
					name           : name,
					id             : id,
					projectId      : projectId,
					description    : description,
					category       : category,
					cronScheduling : cronScheduling,
					isScheduled    : isScheduled,
					isStreaming    : isStreaming,
					alerting       : alerting.toMap(),
			]
		}
	}
	
}
