package com.avadhoot.workforge.issue;

import com.avadhoot.workforge.common.CodeMappings;
import com.avadhoot.workforge.issue.domain.Issue;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.issue.dto.RefDtos.ComponentDto;
import com.avadhoot.workforge.issue.dto.RefDtos.LabelDto;
import com.avadhoot.workforge.issue.dto.RefDtos.StatusDto;
import com.avadhoot.workforge.project.domain.Project;
import com.avadhoot.workforge.project.repository.ProjectRepository;
import com.avadhoot.workforge.sprint.domain.Sprint;
import com.avadhoot.workforge.sprint.repository.SprintRepository;
import com.avadhoot.workforge.user.dto.UserResponse;
import com.avadhoot.workforge.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Assembles the frontend-aligned {@link IssueResponse}, resolving related
 * project, users and sprint into nested payloads.
 */
@Component
public class IssueMapper {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;

    public IssueMapper(ProjectRepository projectRepository, UserRepository userRepository,
                       SprintRepository sprintRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.sprintRepository = sprintRepository;
    }

    public IssueResponse toResponse(Issue issue) {
        Project project = issue.getProjectId() == null ? null
                : projectRepository.findById(issue.getProjectId()).orElse(null);

        UserResponse assignee = issue.getAssigneeId() == null ? null
                : userRepository.findById(issue.getAssigneeId()).map(UserResponse::from).orElse(null);
        UserResponse reporter = issue.getReporterId() == null ? null
                : userRepository.findById(issue.getReporterId()).map(UserResponse::from).orElse(null);

        String sprintName = null;
        if (issue.getSprintId() != null) {
            sprintName = sprintRepository.findById(issue.getSprintId())
                    .map(Sprint::getName).orElse(null);
        }

        List<LabelDto> labels = issue.getLabels() == null ? List.of()
                : issue.getLabels().stream()
                    .sorted(Comparator.comparing(l -> l.getName() == null ? "" : l.getName()))
                    .map(LabelDto::from)
                    .toList();
        List<ComponentDto> components = issue.getComponents() == null ? List.of()
                : issue.getComponents().stream()
                    .sorted(Comparator.comparing(c -> c.getName() == null ? "" : c.getName()))
                    .map(ComponentDto::from)
                    .toList();

        StatusDto status = issue.getStatus() == null ? null : StatusDto.from(issue.getStatus());
        String type = issue.getIssueType() == null ? null
                : CodeMappings.issueTypeCode(issue.getIssueType().getName());
        String priority = issue.getPriority() == null ? null
                : CodeMappings.priorityCode(issue.getPriority().getName());

        return new IssueResponse(
                String.valueOf(issue.getId()),
                issue.getIssueKey(),
                project == null ? null : project.getKey(),
                project == null ? null : project.getName(),
                type,
                issue.getSummary(),
                issue.getDescription(),
                status,
                priority,
                assignee,
                reporter,
                issue.getSprintId() == null ? null : String.valueOf(issue.getSprintId()),
                sprintName,
                labels,
                components,
                issue.getStoryPoints(),
                issue.getDueDate(),
                null,
                issue.getCreatedAt(),
                issue.getUpdatedAt(),
                null);
    }
}
