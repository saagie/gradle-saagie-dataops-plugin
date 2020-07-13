package io.saagie.plugin.models

import io.saagie.plugin.dataops.models.JobVersion
import spock.lang.Specification

class JobVersionTest extends Specification {
	def "JobVersion object DSL should return null values on empty array"() {
		given:
		JobVersion jobVersion = new JobVersion()
		jobVersion.with {
			runtimeVersion = '1'
			releaseNote = 'release note'
			volume = []
			exposedPorts = []
		}
		
		when:
		Map jobVersionMap = jobVersion.toMap()
		
		then:
		jobVersionMap.volume == null
		jobVersionMap.exposedPorts == null
	}
}
