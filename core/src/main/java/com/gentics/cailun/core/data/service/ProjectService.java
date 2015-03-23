package com.gentics.cailun.core.data.service;

import org.springframework.data.neo4j.conversion.Result;

import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.project.request.ProjectCreateRequest;
import com.gentics.cailun.core.rest.project.response.ProjectResponse;

public interface ProjectService extends GenericNodeService<Project> {

	Project findByName(String projectName);

	Project findByUUID(String uuid);

	Result<Project> findAll();

	void deleteByName(String name);

	Project transformFromRest(ProjectCreateRequest requestModel);

	ProjectResponse transformToRest(Project project);

}
