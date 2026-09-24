package com.avadhoot.workforge.search;

import com.avadhoot.workforge.issue.IssueMapper;
import com.avadhoot.workforge.issue.domain.Issue;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.issue.repository.IssueRepository;
import com.avadhoot.workforge.project.ProjectService;
import com.avadhoot.workforge.project.dto.ProjectDtos.ProjectResponse;
import com.avadhoot.workforge.project.repository.ProjectRepository;
import com.avadhoot.workforge.user.dto.UserResponse;
import com.avadhoot.workforge.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SearchService {

    private final IssueRepository issueRepository;
    private final IssueMapper issueMapper;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final UserRepository userRepository;

    public SearchService(IssueRepository issueRepository, IssueMapper issueMapper,
                         ProjectRepository projectRepository, ProjectService projectService,
                         UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.issueMapper = issueMapper;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public SearchController.GlobalSearchResponse search(String q) {
        if (!StringUtils.hasText(q)) {
            return new SearchController.GlobalSearchResponse(List.of(), List.of(), List.of());
        }
        String term = q.trim().toLowerCase();
        String like = "%" + term + "%";

        Specification<Issue> issueSpec = (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("issueKey")), like),
                cb.like(cb.lower(root.get("summary")), like));

        List<IssueResponse> issues = issueRepository
                .findAll(issueSpec, PageRequest.of(0, 10))
                .stream()
                .map(issue -> {
                    // Touch lazy collections while session is open.
                    if (issue.getLabels() != null) {
                        issue.getLabels().size();
                    }
                    if (issue.getComponents() != null) {
                        issue.getComponents().size();
                    }
                    if (issue.getStatus() != null) {
                        issue.getStatus().getName();
                    }
                    if (issue.getPriority() != null) {
                        issue.getPriority().getName();
                    }
                    if (issue.getIssueType() != null) {
                        issue.getIssueType().getName();
                    }
                    return issueMapper.toResponse(issue);
                })
                .toList();

        List<ProjectResponse> projects = projectRepository.findAll().stream()
                .filter(p -> (p.getKey() != null && p.getKey().toLowerCase().contains(term))
                        || (p.getName() != null && p.getName().toLowerCase().contains(term)))
                .limit(10)
                .map(projectService::toResponse)
                .toList();

        List<UserResponse> users = userRepository.findAll().stream()
                .filter(u -> (u.getUsername() != null && u.getUsername().toLowerCase().contains(term))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(term))
                        || (u.getFullName() != null && u.getFullName().toLowerCase().contains(term)))
                .limit(10)
                .map(UserResponse::from)
                .toList();

        return new SearchController.GlobalSearchResponse(issues, projects, users);
    }
}
