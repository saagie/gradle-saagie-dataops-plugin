package io.saagie.plugin.dataops.models

import groovy.transform.TypeChecked

@TypeChecked
class ExportPipeline implements IExists {
	
	PipelineDTO pipelineDTO = new PipelineDTO()
	PipelineVersionDTO pipelineVersionDTO = new PipelineVersionDTO()
	ArrayList<PipelineVersionDTO> versions = new ArrayList<PipelineVersionDTO>()
	
	@Override
	boolean exists() {
		return pipelineDTO.exists() || pipelineVersionDTO.exists()
	}
	
	void setPipelineFromApiResult( pipelineDetailResult ) {
		pipelineDTO.setPipelineFromApiResult(pipelineDetailResult)
	}
	
	void setPipelineFromV1ApiResult( pipelineDetailResult ) {
		pipelineDTO.setPipelineFromV1ApiResult(pipelineDetailResult)
	}
	
	void setPipelineVersionFromV1ApiResult( jobs, String releaseNote, String number = null ) {
		pipelineVersionDTO.setPipelineVersionFromV1ApiResult(jobs, releaseNote, number)
	}
	
	void setPipelineVersionFromApiResult( version ) {
		pipelineVersionDTO.setPipelineVersionFromApiResult(version)
	}
	
	void addPipelineVersionDtoToVersions( jobs, String releaseNote, String number = null ) {
		PipelineVersionDTO pipelineVersionDTO = new PipelineVersionDTO()
		pipelineVersionDTO.setPipelineVersionFromV1ApiResult(jobs, releaseNote, number)
		versions.add(0, pipelineVersionDTO)
	}
}
