package com.avadhoot.workforge.project;

import com.avadhoot.workforge.board.BoardService;
import com.avadhoot.workforge.board.dto.BoardDtos.ProjectBoardData;
import com.avadhoot.workforge.common.dto.ApiResponse;
import com.avadhoot.workforge.common.dto.PageResponse;
import com.avadhoot.workforge.issue.IssueService;
import com.avadhoot.workforge.issue.LabelComponentService;
import com.avadhoot.workforge.issue.dto.IssueDtos.CreateIssueRequest;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.issue.dto.LabelComponentDtos.ComponentResponse;
import com.avadhoot.workforge.issue.dto.LabelComponentDtos.LabelResponse;
import com.avadhoot.workforge.issue.dto.RefDtos.StatusDto;
import com.avadhoot.workforge.sprint.SprintService;
import com.avadhoot.workforge.sprint.dto.SprintDtos.BacklogData;
import com.avadhoot.workforge.sprint.dto.SprintDtos.CreateSprintByKeyRequest;
import com.avadhoot.workforge.sprint.dto.SprintDtos.SprintResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Project-key scoped facade endpoints matching the React client contract.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectKey}")
public class ProjectWorkspaceController {

    private final IssueService issueService;
    private final SprintService sprintService;
    private final BoardService boardService;
    private final LabelComponentService labelComponentService;
    private final ProjectService projectService;

    public ProjectWorkspaceController(IssueService issueService, SprintService sprintService,
                                      BoardService boardService, LabelComponentService labelComponentService,
                                      ProjectService projectService) {
        this.issueService = issueService;
        this.sprintService = sprintService;
        this.boardService = boardService;
        this.labelComponentService = labelComponentService;
        this.projectService = projectService;
    }

    @PostMapping("/issues")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ISSUE_CREATE')")
    public ApiResponse<IssueResponse> createIssue(@PathVariable String projectKey,
                                                  @Valid @RequestBody CreateIssueRequest request) {
        return ApiResponse.success(issueService.create(projectKey, request), "Issue created");
    }

    @GetMapping("/issues")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<PageResponse<IssueResponse>> listIssues(
            @PathVariable String projectKey,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) String reporterId,
            @RequestParam(required = false) String sprintId,
            Pageable pageable) {
        return ApiResponse.success(issueService.list(projectKey, search, status, type, priority,
                assigneeId, reporterId, sprintId, pageable));
    }

    @GetMapping("/board")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<ProjectBoardData> board(@PathVariable String projectKey,
                                               @RequestParam(required = false) String sprintId) {
        return ApiResponse.success(boardService.projectBoard(projectKey, sprintId));
    }

    @GetMapping("/backlog")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<BacklogData> backlog(@PathVariable String projectKey) {
        return ApiResponse.success(sprintService.backlogData(projectKey));
    }

    @GetMapping("/sprints")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ApiResponse<List<SprintResponse>> sprints(@PathVariable String projectKey) {
        return ApiResponse.success(sprintService.listByProjectKey(projectKey));
    }

    @PostMapping("/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SPRINT_MANAGE')")
    public ApiResponse<SprintResponse> createSprint(@PathVariable String projectKey,
                                                    @Valid @RequestBody CreateSprintByKeyRequest request) {
        return ApiResponse.success(sprintService.createByProjectKey(projectKey, request), "Sprint created");
    }

    @GetMapping("/statuses")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ApiResponse<List<StatusDto>> statuses(@PathVariable String projectKey) {
        projectService.resolveProject(projectKey);
        return ApiResponse.success(boardService.workflowStatuses(projectKey));
    }

    @GetMapping("/labels")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ApiResponse<List<LabelResponse>> labels(@PathVariable String projectKey) {
        Long projectId = projectService.resolveProject(projectKey).getId();
        return ApiResponse.success(labelComponentService.labels(projectId));
    }

    @GetMapping("/components")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ApiResponse<List<ComponentResponse>> components(@PathVariable String projectKey) {
        Long projectId = projectService.resolveProject(projectKey).getId();
        return ApiResponse.success(labelComponentService.components(projectId));
    }
}
