package com.avadhoot.workforgeai.ai.tools.service;

import com.avadhoot.workforgeai.ai.tools.model.IssueRecord;
import com.avadhoot.workforgeai.ai.tools.model.ProjectRecord;
import com.avadhoot.workforgeai.ai.tools.repository.WorkforgeSampleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class IssueToolService {

    private final WorkforgeSampleRepository repository;
    private final int maxLimit;
    private final int defaultLimit;

    public IssueToolService(
            WorkforgeSampleRepository repository,
            @Value("${workforge.ai.tools.max-search-limit:50}") int maxLimit,
            @Value("${workforge.ai.tools.default-search-limit:10}") int defaultLimit) {
        this.repository = repository;
        this.maxLimit = maxLimit;
        this.defaultLimit = defaultLimit;
    }

    public Optional<IssueRecord> getIssue(String issueKey) {
        if (!StringUtils.hasText(issueKey)) {
            throw new IllegalArgumentException("issueKey is required");
        }
        return repository.findIssueByKey(issueKey.trim());
    }

    public List<IssueRecord> searchIssues(String projectKey, String status, String assignee, Integer limit) {
        int resolved = resolveLimit(limit);
        return repository.searchIssues(
                blankToNull(projectKey),
                blankToNull(status),
                blankToNull(assignee),
                resolved);
    }

    private int resolveLimit(Integer limit) {
        int value = limit == null ? defaultLimit : limit;
        if (value <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0");
        }
        if (value > maxLimit) {
            throw new IllegalArgumentException("limit must be at most " + maxLimit);
        }
        return value;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
