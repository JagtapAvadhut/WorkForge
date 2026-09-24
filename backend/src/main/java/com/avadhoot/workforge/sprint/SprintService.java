package com.avadhoot.workforge.sprint;

import com.avadhoot.workforge.common.CodeMappings;
import com.avadhoot.workforge.exception.BusinessException;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.issue.IssueMapper;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.issue.repository.IssueRepository;
import com.avadhoot.workforge.project.domain.Project;
import com.avadhoot.workforge.project.repository.ProjectRepository;
import com.avadhoot.workforge.sprint.domain.Sprint;
import com.avadhoot.workforge.sprint.domain.SprintState;
import com.avadhoot.workforge.sprint.dto.SprintDtos.BacklogData;
import com.avadhoot.workforge.sprint.dto.SprintDtos.CreateSprintByKeyRequest;
import com.avadhoot.workforge.sprint.dto.SprintDtos.CreateSprintRequest;
import com.avadhoot.workforge.sprint.dto.SprintDtos.SprintBucket;
import com.avadhoot.workforge.sprint.dto.SprintDtos.SprintResponse;
import com.avadhoot.workforge.sprint.dto.SprintDtos.StartSprintRequest;
import com.avadhoot.workforge.sprint.dto.SprintDtos.UpdateSprintRequest;
import com.avadhoot.workforge.sprint.repository.SprintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final IssueMapper issueMapper;
    private final ProjectRepository projectRepository;

    public SprintService(SprintRepository sprintRepository, IssueRepository issueRepository,
                         IssueMapper issueMapper, ProjectRepository projectRepository) {
        this.sprintRepository = sprintRepository;
        this.issueRepository = issueRepository;
        this.issueMapper = issueMapper;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public SprintResponse create(CreateSprintRequest request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", request.projectId()));
        return createInternal(project, request.name(), request.goal(), null, null);
    }

    @Transactional
    public SprintResponse createByProjectKey(String projectKey, CreateSprintByKeyRequest request) {
        Project project = resolveProject(projectKey);
        return createInternal(project, request.name(), request.goal(), request.startDate(), request.endDate());
    }

    private SprintResponse createInternal(Project project, String name, String goal,
                                          Instant startDate, Instant endDate) {
        Sprint sprint = new Sprint();
        sprint.setProjectId(project.getId());
        sprint.setName(name);
        sprint.setGoal(goal);
        sprint.setState(SprintState.FUTURE);
        sprint.setStartDate(startDate);
        sprint.setEndDate(endDate);
        return SprintResponse.from(sprintRepository.save(sprint), project.getKey());
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> list(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        return sprintRepository.findByProjectId(projectId).stream()
                .map(s -> SprintResponse.from(s, project.getKey(), issueRepository.findBySprintId(s.getId()).size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> listByProjectKey(String projectKey) {
        Project project = resolveProject(projectKey);
        return list(project.getId());
    }

    @Transactional
    public SprintResponse update(Long id, UpdateSprintRequest request) {
        Sprint sprint = findById(id);
        Project project = projectRepository.findById(sprint.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", sprint.getProjectId()));
        if (request.name() != null) {
            sprint.setName(request.name());
        }
        if (request.goal() != null) {
            sprint.setGoal(request.goal());
        }
        if (request.startDate() != null) {
            sprint.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            sprint.setEndDate(request.endDate());
        }
        return SprintResponse.from(sprint, project.getKey());
    }

    @Transactional
    public SprintResponse start(Long id, StartSprintRequest request) {
        Sprint sprint = findById(id);
        if (sprint.getState() != SprintState.FUTURE) {
            throw new BusinessException("Only planned sprints can be started");
        }
        List<Sprint> active = sprintRepository.findByProjectIdAndState(sprint.getProjectId(), SprintState.ACTIVE);
        if (!active.isEmpty()) {
            throw new BusinessException("Only one active sprint is allowed per project");
        }
        Project project = projectRepository.findById(sprint.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", sprint.getProjectId()));
        sprint.setState(SprintState.ACTIVE);
        if (request != null) {
            if (request.startDate() != null) {
                sprint.setStartDate(request.startDate());
            }
            if (request.endDate() != null) {
                sprint.setEndDate(request.endDate());
            }
        }
        if (sprint.getStartDate() == null) {
            sprint.setStartDate(Instant.now());
        }
        return SprintResponse.from(sprint, project.getKey());
    }

    @Transactional
    public SprintResponse complete(Long id, String moveToSprintId) {
        Sprint sprint = findById(id);
        if (sprint.getState() != SprintState.ACTIVE) {
            throw new BusinessException("Only active sprints can be completed");
        }
        Project project = projectRepository.findById(sprint.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", sprint.getProjectId()));
        sprint.setState(SprintState.CLOSED);
        Long targetSprint = CodeMappings.parseId(moveToSprintId);
        issueRepository.findBySprintId(id).forEach(issue -> {
            if (issue.getStatus() != null
                    && issue.getStatus().getCategory() != com.avadhoot.workforge.issue.domain.StatusCategory.DONE) {
                issue.setSprintId(targetSprint);
            }
        });
        return SprintResponse.from(sprint, project.getKey());
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> issues(Long sprintId) {
        return issueRepository.findBySprintId(sprintId).stream().map(issueMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> backlog(Long projectId) {
        return issueRepository.findByProjectIdAndSprintIdIsNull(projectId).stream()
                .map(issueMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BacklogData backlogData(String projectKey) {
        Project project = resolveProject(projectKey);
        List<IssueResponse> backlogIssues = backlog(project.getId());
        List<SprintBucket> buckets = new ArrayList<>();
        for (Sprint sprint : sprintRepository.findByProjectId(project.getId())) {
            if (sprint.getState() == SprintState.CLOSED) {
                continue;
            }
            List<IssueResponse> sprintIssues = issues(sprint.getId());
            buckets.add(new SprintBucket(
                    SprintResponse.from(sprint, project.getKey(), sprintIssues.size()),
                    sprintIssues));
        }
        return new BacklogData(backlogIssues, buckets);
    }

    private Project resolveProject(String projectKey) {
        return projectRepository.findByKey(projectKey)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectKey));
    }

    private Sprint findById(Long id) {
        return sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", id));
    }
}
