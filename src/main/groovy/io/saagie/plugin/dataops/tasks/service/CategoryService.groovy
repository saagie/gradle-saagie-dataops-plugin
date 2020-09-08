package io.saagie.plugin.dataops.tasks.service

import io.saagie.plugin.dataops.tasks.projects.enums.JobV1Category
import io.saagie.plugin.dataops.tasks.projects.enums.JobV2Category
import org.gradle.api.GradleException

@Singleton
class CategoryService {
	private final ArrayList<Map> categoryValuesPerTechnology = [
			[
					label : "Bash",
					value : new ArrayList([0, 1, 2])
			],
			[
					label : "Python",
					value : new ArrayList([0, 1, 2]),
			],
			[
					label : "SQOOP",
					value : new ArrayList([0, 1, 2]),
			],
			[
					label : "Java/Scala",
					value : new ArrayList([0, 1, 2]),
			],
			[
					label : "Spark",
					value : new ArrayList([0, 1, 2]),
			],
			[
					label : "Docker",
					value : new ArrayList([0, 1, 2]),
			],
			[
					label : "R",
					value : new ArrayList([0, 1, 2]),
			],
			[
					label : "Talend",
					value : new ArrayList([0, 1, 2]),
			]]
	
	public String getCategoryByV1CategoryAndTechnology( String categoryV1Name, String technologyV2Name ) {
		def categoryValuesPerTechnologyFound = categoryValuesPerTechnology.find { it.label.equals(technologyV2Name) }
		def categoryV1PerTechnologyV1 = Enum.valueOf(JobV1Category.class, categoryV1Name)
		def indexCategoryV1PerTechnologyV1 = categoryV1PerTechnologyV1.value
		
		if (!indexCategoryV1PerTechnologyV1 && indexCategoryV1PerTechnologyV1 != 0) {
			throw new GradleException("Couldn't find category v1 from enum for params categoryV1Name: ${categoryV1Name} and technologyV2Name: ${technologyV2Name}")
		}
		
		def indexCategoryValuesPerTechnologyFound = null
		
		if (!categoryValuesPerTechnologyFound?.value) {
			indexCategoryValuesPerTechnologyFound = 0
		} else {
			indexCategoryValuesPerTechnologyFound = categoryValuesPerTechnologyFound.value.contains(indexCategoryV1PerTechnologyV1)
			if (!indexCategoryValuesPerTechnologyFound && indexCategoryValuesPerTechnologyFound != 0) {
				indexCategoryValuesPerTechnologyFound = categoryValuesPerTechnologyFound.value.contains(1)
				if (!indexCategoryValuesPerTechnologyFound && indexCategoryValuesPerTechnologyFound != 0) {
					indexCategoryValuesPerTechnologyFound = 0
				}
			}
		}
		
		return JobV2Category.getPerValue(indexCategoryV1PerTechnologyV1)
	}
}
