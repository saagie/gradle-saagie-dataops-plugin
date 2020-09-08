package io.saagie.plugin.dataops.models

class ImportJob implements IExists, IMapable {
	
	/**
	 * The file path to import
	 */
	String import_file
	
	String temporary_directory
	
	@Override
	boolean exists() {
		return import_file
	}
	
	@Override
	Map toMap() {
		if (!exists()) return null
		return [import_file : import_file]
	}
}
