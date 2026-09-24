package com.avadhoot.workforgeai.ai.tools.service;

import com.avadhoot.workforgeai.ai.tools.model.ProjectRecord;
import com.avadhoot.workforgeai.ai.tools.repository.WorkforgeSampleRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class ProjectToolService {

    private final WorkforgeSampleRepository repository;

    public ProjectToolService(WorkforgeSampleRepository repository) {
        this.repository = repository;
    }

    public Optional<ProjectRecord> getProject(String projectKey) {
        if (!StringUtils.hasText(projectKey)) {
            throw new IllegalArgumentException("projectKey is required");
        }
        return repository.findProjectByKey(projectKey.trim());
    }
}
