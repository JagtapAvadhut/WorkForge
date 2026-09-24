package com.avadhoot.workforge.issue;

import com.avadhoot.workforge.audit.event.IssueEvents.IssueAssignedEvent;
import com.avadhoot.workforge.audit.event.IssueEvents.IssueCreatedEvent;
import com.avadhoot.workforge.audit.event.IssueEvents.IssueTransitionedEvent;
import com.avadhoot.workforge.audit.event.IssueEvents.IssueUpdatedEvent;
import com.avadhoot.workforge.common.CodeMappings;
import com.avadhoot.workforge.common.dto.PageResponse;
import com.avadhoot.workforge.exception.BusinessException;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.issue.domain.Component;
import com.avadhoot.workforge.issue.domain.Issue;
import com.avadhoot.workforge.issue.domain.IssueType;
import com.avadhoot.workforge.issue.domain.Label;
import com.avadhoot.workforge.issue.domain.Priority;
import com.avadhoot.workforge.issue.domain.Status;
import com.avadhoot.workforge.issue.domain.StatusCategory;
import com.avadhoot.workforge.issue.dto.IssueDtos.CreateIssueRequest;
import com.avadhoot.workforge.issue.dto.IssueDtos.UpdateIssueRequest;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.issue.repository.ComponentRepository;
import com.avadhoot.workforge.issue.repository.IssueRepository;
import com.avadhoot.workforge.issue.repository.IssueTypeRepository;
import com.avadhoot.workforge.issue.repository.LabelRepository;
import com.avadhoot.workforge.issue.repository.PriorityRepository;
import com.avadhoot.workforge.issue.repository.StatusRepository;
import com.avadhoot.workforge.issue.search.IssueQueryParser;
import com.avadhoot.workforge.issue.search.IssueSpecifications;
import com.avadhoot.workforge.project.IssueSequenceService;
import com.avadhoot.workforge.project.domain.Project;
import com.avadhoot.workforge.project.repository.ProjectRepository;
import com.avadhoot.workforge.security.SecurityUtils;
import com.avadhoot.workforge.workflow.WorkflowService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final PriorityRepository priorityRepository;
    private final StatusRepository statusRepository;
    private final ProjectRepository projectRepository;
    private final LabelRepository labelRepository;
    private final ComponentRepository componentRepository;
    private final IssueSequenceService issueSequenceService;
    private final WorkflowService workflowService;
    private final IssueMapper issueMapper;
    private final IssueQueryParser queryParser;
    private final ApplicationEventPublisher eventPublisher;

    public IssueService(IssueRepository issueRepository, IssueTypeRepository issueTypeRepository,
                        PriorityRepository priorityRepository, StatusRepository statusRepository,
                        ProjectRepository projectRepository, LabelRepository labelRepository,
                        ComponentRepository componentRepository, IssueSequenceService issueSequenceService,
                        WorkflowService workflowService, IssueMapper issueMapper,
                        IssueQueryParser queryParser, ApplicationEventPublisher eventPublisher) {
        this.issueRepository = issueRepository;
        this.issueTypeRepository = issueTypeRepository;
        this.priorityRepository = priorityRepository;
        this.statusRepository = statusRepository;
        this.projectRepository = projectRepository;
        this.labelRepository = labelRepository;
        this.componentRepository = componentRepository;
        this.issueSequenceService = issueSequenceService;
        this.workflowService = workflowService;
        this.issueMapper = issueMapper;
        this.queryParser = queryParser;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public IssueResponse create(CreateIssueRequest request) {
        return create(request.projectKey(), request);
    }

    @Transactional
    public IssueResponse create(String projectKey, CreateIssueRequest request) {
        String key = projectKey != null ? projectKey : request.projectKey();
        if (!StringUtils.hasText(key)) {
            throw new BusinessException("projectKey is required to create an issue");
        }
        Project project = resolveProject(key);

        IssueType type = StringUtils.hasText(request.type())
                ? issueTypeRepository.findByNameIgnoreCase(CodeMappings.issueTypeName(request.type()))
                    .orElseThrow(() -> new ResourceNotFoundException("IssueType", request.type()))
                : issueTypeRepository.findByNameIgnoreCase("Task")
                    .orElseThrow(() -> new ResourceNotFoundException("Default issue type 'Task' missing"));

        Priority priority = StringUtils.hasText(request.priority())
                ? priorityRepository.findByNameIgnoreCase(CodeMappings.priorityName(request.priority()))
                    .orElseThrow(() -> new ResourceNotFoundException("Priority", request.priority()))
                : priorityRepository.findByNameIgnoreCase("Medium")
                    .orElseThrow(() -> new ResourceNotFoundException("Default priority 'Medium' missing"));

        Long workflowId = project.getWorkflowId() != null
                ? project.getWorkflowId() : workflowService.defaultWorkflow().getId();
        Status initialStatus = statusRepository.findById(workflowService.initialStatusId(workflowId))
                .orElseThrow(() -> new ResourceNotFoundException("Initial status missing"));

        Long parentId = CodeMappings.parseId(request.parentId());
        if (parentId != null && issueRepository.findById(parentId).isEmpty()) {
            throw new ResourceNotFoundException("Parent issue", parentId);
        }

        Long assigneeId = CodeMappings.parseId(request.assigneeId());
        Long sprintId = CodeMappings.parseId(request.sprintId());

        long seq = issueSequenceService.next(project.getId());
        Long reporterId = SecurityUtils.currentUserId()
                .or(() -> java.util.Optional.ofNullable(assigneeId))
                .or(() -> java.util.Optional.ofNullable(project.getLeadId()))
                .orElse(null);
        if (reporterId == null) {
            throw new BusinessException("Reporter is required to create an issue");
        }

        Issue issue = new Issue();
        issue.setProjectId(project.getId());
        issue.setIssueKey(project.getKey() + "-" + seq);
        issue.setSummary(request.summary());
        issue.setDescription(request.description());
        issue.setIssueType(type);
        issue.setPriority(priority);
        issue.setStatus(initialStatus);
        issue.setReporterId(reporterId);
        issue.setAssigneeId(assigneeId);
        issue.setParentId(parentId);
        issue.setSprintId(sprintId);
        issue.setStoryPoints(request.storyPoints());
        issue.setDueDate(request.dueDate());
        issue.setLabels(resolveLabels(request.labelIds()));
        issue.setComponents(resolveComponents(request.componentIds()));

        issue = issueRepository.save(issue);
        eventPublisher.publishEvent(new IssueCreatedEvent(
                issue.getId(), issue.getIssueKey(), reporterId, issue.getAssigneeId()));
        return issueMapper.toResponse(issue);
    }

    @Transactional(readOnly = true)
    public IssueResponse getByKey(String key) {
        return issueMapper.toResponse(findByKey(key));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> search(String jql, Pageable pageable) {
        Specification<Issue> spec = queryParser.parse(jql);
        return PageResponse.of(issueRepository.findAll(spec, pageable).map(issueMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> list(String projectKey, String search, String status, String type,
                                            String priority, String assigneeId, String reporterId,
                                            String sprintId, Pageable pageable) {
        Specification<Issue> spec = Specification.unrestricted();
        if (StringUtils.hasText(projectKey)) {
            spec = spec.and(IssueSpecifications.projectId(resolveProject(projectKey).getId()));
        }
        if (StringUtils.hasText(search)) {
            spec = spec.and(IssueSpecifications.summaryContains(search));
        }
        if (StringUtils.hasText(status)) {
            spec = spec.and(statusSpec(status));
        }
        if (StringUtils.hasText(type)) {
            spec = spec.and(IssueSpecifications.typeName(CodeMappings.issueTypeName(type)));
        }
        if (StringUtils.hasText(priority)) {
            spec = spec.and(IssueSpecifications.priorityName(CodeMappings.priorityName(priority)));
        }
        Long assignee = CodeMappings.parseId(assigneeId);
        if (assignee != null) {
            spec = spec.and(IssueSpecifications.assigneeId(assignee));
        }
        Long reporter = CodeMappings.parseId(reporterId);
        if (reporter != null) {
            spec = spec.and(IssueSpecifications.reporterId(reporter));
        }
        Long sprint = CodeMappings.parseId(sprintId);
        if (sprint != null) {
            spec = spec.and(IssueSpecifications.sprintId(sprint));
        }
        return PageResponse.of(issueRepository.findAll(spec, pageable).map(issueMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> myWork(Pageable pageable) {
        Long me = SecurityUtils.requireCurrentUserId();
        Specification<Issue> spec = IssueSpecifications.assigneeId(me);
        return PageResponse.of(issueRepository.findAll(spec, pageable).map(issueMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> subtasksByKey(String key) {
        Issue parent = findByKey(key);
        return issueRepository.findByParentId(parent.getId()).stream()
                .map(issueMapper::toResponse)
                .toList();
    }

    @Transactional
    public IssueResponse update(String key, UpdateIssueRequest request) {
        Issue issue = findByKey(key);
        if (request.summary() != null) {
            issue.setSummary(request.summary());
        }
        if (request.description() != null) {
            issue.setDescription(request.description());
        }
        if (StringUtils.hasText(request.type())) {
            issue.setIssueType(issueTypeRepository.findByNameIgnoreCase(CodeMappings.issueTypeName(request.type()))
                    .orElseThrow(() -> new ResourceNotFoundException("IssueType", request.type())));
        }
        if (StringUtils.hasText(request.priority())) {
            issue.setPriority(priorityRepository.findByNameIgnoreCase(CodeMappings.priorityName(request.priority()))
                    .orElseThrow(() -> new ResourceNotFoundException("Priority", request.priority())));
        }
        if (request.assigneeId() != null) {
            issue.setAssigneeId(CodeMappings.parseId(request.assigneeId()));
        }
        if (request.sprintId() != null) {
            issue.setSprintId(CodeMappings.parseId(request.sprintId()));
        }
        if (request.storyPoints() != null) {
            issue.setStoryPoints(request.storyPoints());
        }
        if (request.dueDate() != null) {
            issue.setDueDate(request.dueDate());
        }
        if (request.labelIds() != null) {
            issue.setLabels(resolveLabels(request.labelIds()));
        }
        if (request.componentIds() != null) {
            issue.setComponents(resolveComponents(request.componentIds()));
        }
        if (StringUtils.hasText(request.statusId())) {
            applyTransition(issue, CodeMappings.parseId(request.statusId()));
        }
        publishUpdate(issue, "Issue fields updated");
        return issueMapper.toResponse(issue);
    }

    @Transactional
    public IssueResponse assign(String key, String assigneeId) {
        Issue issue = findByKey(key);
        Long id = CodeMappings.parseId(assigneeId);
        issue.setAssigneeId(id);
        eventPublisher.publishEvent(new IssueAssignedEvent(
                issue.getId(), issue.getIssueKey(), currentUser(), id));
        return issueMapper.toResponse(issue);
    }

    @Transactional
    public IssueResponse transition(String key, Long targetStatusId) {
        Issue issue = findByKey(key);
        applyTransition(issue, targetStatusId);
        return issueMapper.toResponse(issue);
    }

    private void applyTransition(Issue issue, Long targetStatusId) {
        if (targetStatusId == null) {
            throw new BusinessException("statusId is required");
        }
        Project project = projectRepository.findById(issue.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", issue.getProjectId()));
        Long workflowId = project.getWorkflowId() != null
                ? project.getWorkflowId() : workflowService.defaultWorkflow().getId();
        Long currentStatusId = issue.getStatus().getId();

        workflowService.validateTransition(workflowId, currentStatusId, targetStatusId);

        Status target = statusRepository.findById(targetStatusId)
                .orElseThrow(() -> new ResourceNotFoundException("Status", targetStatusId));
        String from = issue.getStatus().getName();
        issue.setStatus(target);
        eventPublisher.publishEvent(new IssueTransitionedEvent(
                issue.getId(), issue.getIssueKey(), currentUser(), from, target.getName()));
    }

    @Transactional
    public IssueResponse changePriority(String key, String priorityCode) {
        Issue issue = findByKey(key);
        issue.setPriority(priorityRepository.findByNameIgnoreCase(CodeMappings.priorityName(priorityCode))
                .orElseThrow(() -> new ResourceNotFoundException("Priority", priorityCode)));
        publishUpdate(issue, "Priority changed");
        return issueMapper.toResponse(issue);
    }

    @Transactional
    public IssueResponse changeSprint(String key, String sprintId) {
        Issue issue = findByKey(key);
        Long id = CodeMappings.parseId(sprintId);
        issue.setSprintId(id);
        publishUpdate(issue, id == null ? "Removed from sprint" : "Moved to sprint " + id);
        return issueMapper.toResponse(issue);
    }

    @Transactional
    public IssueResponse addLabel(String key, Long labelId) {
        Issue issue = findByKey(key);
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", labelId));
        issue.getLabels().add(label);
        return issueMapper.toResponse(issue);
    }

    @Transactional
    public IssueResponse removeLabel(String key, Long labelId) {
        Issue issue = findByKey(key);
        issue.getLabels().removeIf(l -> l.getId().equals(labelId));
        return issueMapper.toResponse(issue);
    }

    @Transactional
    public IssueResponse addComponent(String key, Long componentId) {
        Issue issue = findByKey(key);
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Component", componentId));
        issue.getComponents().add(component);
        return issueMapper.toResponse(issue);
    }

    @Transactional
    public IssueResponse removeComponent(String key, Long componentId) {
        Issue issue = findByKey(key);
        issue.getComponents().removeIf(c -> c.getId().equals(componentId));
        return issueMapper.toResponse(issue);
    }

    @Transactional
    public void delete(String key) {
        Issue issue = findByKey(key);
        if (!issueRepository.findByParentId(issue.getId()).isEmpty()) {
            throw new BusinessException("Cannot delete an issue that has subtasks");
        }
        issueRepository.delete(issue);
    }

    public Issue findByKey(String key) {
        return issueRepository.findByIssueKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", key));
    }

    private Specification<Issue> statusSpec(String status) {
        Long statusId = CodeMappings.parseId(status);
        if (statusId != null) {
            return IssueSpecifications.statusId(statusId);
        }
        String upper = status.trim().toUpperCase().replace(' ', '_');
        for (StatusCategory category : StatusCategory.values()) {
            if (category.name().equals(upper)) {
                return IssueSpecifications.statusCategory(category);
            }
        }
        return IssueSpecifications.statusName(status);
    }

    private Project resolveProject(String keyOrId) {
        Long id = CodeMappings.parseId(keyOrId);
        if (id != null) {
            return projectRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", keyOrId));
        }
        return projectRepository.findByKey(keyOrId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", keyOrId));
    }

    private void publishUpdate(Issue issue, String summary) {
        eventPublisher.publishEvent(new IssueUpdatedEvent(
                issue.getId(), issue.getIssueKey(), currentUser(), summary));
    }

    private Long currentUser() {
        return SecurityUtils.currentUserId().orElse(null);
    }

    private Set<Label> resolveLabels(List<String> ids) {
        Set<Label> labels = new HashSet<>();
        if (ids != null) {
            ids.stream().map(CodeMappings::parseId).filter(java.util.Objects::nonNull).forEach(labelId ->
                    labels.add(labelRepository.findById(labelId)
                            .orElseThrow(() -> new ResourceNotFoundException("Label", labelId))));
        }
        return labels;
    }

    private Set<Component> resolveComponents(List<String> ids) {
        Set<Component> components = new HashSet<>();
        if (ids != null) {
            ids.stream().map(CodeMappings::parseId).filter(java.util.Objects::nonNull).forEach(componentId ->
                    components.add(componentRepository.findById(componentId)
                            .orElseThrow(() -> new ResourceNotFoundException("Component", componentId))));
        }
        return components;
    }
}
