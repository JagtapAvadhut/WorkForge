package com.avadhoot.workforge.project;

import com.avadhoot.workforge.board.BoardService;
import com.avadhoot.workforge.common.CodeMappings;
import com.avadhoot.workforge.common.dto.PageResponse;
import com.avadhoot.workforge.exception.DuplicateResourceException;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.project.domain.Project;
import com.avadhoot.workforge.project.domain.ProjectMember;
import com.avadhoot.workforge.project.domain.ProjectMemberRole;
import com.avadhoot.workforge.project.dto.ProjectDtos.AddMemberRequest;
import com.avadhoot.workforge.project.dto.ProjectDtos.CreateProjectRequest;
import com.avadhoot.workforge.project.dto.ProjectDtos.ProjectMemberResponse;
import com.avadhoot.workforge.project.dto.ProjectDtos.ProjectResponse;
import com.avadhoot.workforge.project.dto.ProjectDtos.UpdateProjectRequest;
import com.avadhoot.workforge.project.repository.ProjectMemberRepository;
import com.avadhoot.workforge.project.repository.ProjectRepository;
import com.avadhoot.workforge.security.SecurityUtils;
import com.avadhoot.workforge.user.domain.User;
import com.avadhoot.workforge.user.dto.UserResponse;
import com.avadhoot.workforge.user.repository.UserRepository;
import com.avadhoot.workforge.workflow.WorkflowService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final IssueSequenceService issueSequenceService;
    private final WorkflowService workflowService;
    private final BoardService boardService;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository memberRepository,
                          UserRepository userRepository, IssueSequenceService issueSequenceService,
                          WorkflowService workflowService, BoardService boardService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.issueSequenceService = issueSequenceService;
        this.workflowService = workflowService;
        this.boardService = boardService;
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        if (projectRepository.existsByKey(request.key())) {
            throw new DuplicateResourceException("Project key already exists: " + request.key());
        }
        Project project = new Project();
        project.setKey(request.key());
        project.setName(request.name());
        project.setDescription(request.description());
        project.setLeadId(CodeMappings.parseId(request.leadId()));
        project.setOrganizationId(CodeMappings.parseId(request.organizationId()));
        project.setWorkflowId(workflowService.defaultWorkflow().getId());
        project = projectRepository.save(project);

        issueSequenceService.initialise(project.getId());
        boardService.ensureDefaultBoard(project);

        Long creator = SecurityUtils.currentUserId().orElse(project.getLeadId());
        if (creator != null) {
            addMemberInternal(project.getId(), creator, ProjectMemberRole.PROJECT_ADMIN);
        }
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(String keyOrId) {
        return toResponse(resolveProject(keyOrId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(Pageable pageable) {
        return PageResponse.of(projectRepository.findAll(pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listAll() {
        return projectRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProjectResponse update(String keyOrId, UpdateProjectRequest request) {
        Project project = resolveProject(keyOrId);
        if (request.name() != null) {
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.leadId() != null) {
            project.setLeadId(CodeMappings.parseId(request.leadId()));
        }
        if (request.enabled() != null) {
            project.setEnabled(request.enabled());
        }
        return toResponse(project);
    }

    @Transactional
    public void delete(String keyOrId) {
        Project project = resolveProject(keyOrId);
        project.setEnabled(false);
    }

    @Transactional
    public ProjectMemberResponse addMember(String keyOrId, AddMemberRequest request) {
        Project project = resolveProject(keyOrId);
        Long userId = CodeMappings.parseId(request.userId());
        if (userId == null) {
            throw new ResourceNotFoundException("User", request.userId());
        }
        if (memberRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            throw new DuplicateResourceException("User is already a member of this project");
        }
        return toMemberResponse(
                addMemberInternal(project.getId(), userId, CodeMappings.projectMemberRole(request.role())));
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> members(String keyOrId) {
        Project project = resolveProject(keyOrId);
        return memberRepository.findByProjectId(project.getId()).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public void removeMember(String keyOrId, String userId) {
        Project project = resolveProject(keyOrId);
        Long uid = CodeMappings.parseId(userId);
        ProjectMember member = memberRepository.findByProjectIdAndUserId(project.getId(), uid)
                .orElseThrow(() -> new ResourceNotFoundException("Project member", userId));
        memberRepository.delete(member);
    }

    private ProjectMember addMemberInternal(Long projectId, Long userId, ProjectMemberRole role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(role);
        return memberRepository.save(member);
    }

    /** Resolve a project by numeric id or by its key. */
    @Transactional(readOnly = true)
    public Project resolveProject(String keyOrId) {
        Long id = CodeMappings.parseId(keyOrId);
        if (id != null) {
            return projectRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", keyOrId));
        }
        return projectRepository.findByKey(keyOrId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", keyOrId));
    }

    public ProjectResponse toResponse(Project project) {
        UserResponse lead = null;
        if (project.getLeadId() != null) {
            lead = userRepository.findById(project.getLeadId()).map(UserResponse::from).orElse(null);
        }
        int memberCount = memberRepository.findByProjectId(project.getId()).size();
        return new ProjectResponse(
                String.valueOf(project.getId()),
                project.getKey(),
                project.getName(),
                project.getDescription(),
                lead,
                memberCount,
                null,
                null,
                null,
                project.getCreatedAt());
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember member) {
        User user = userRepository.findById(member.getUserId()).orElse(null);
        return new ProjectMemberResponse(
                UserResponse.from(user),
                CodeMappings.projectRoleCode(member.getRole()),
                member.getCreatedAt());
    }
}
