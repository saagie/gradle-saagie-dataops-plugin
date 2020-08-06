package io.saagie.plugin.dataops.models

import javax.validation.constraints.NotBlank

class EnvVariable {
	
	@NotBlank(message = 'scope cannot be empty')
	String scope
	
	ArrayList<String> name
	
	boolean include_all_var = false
	
}
