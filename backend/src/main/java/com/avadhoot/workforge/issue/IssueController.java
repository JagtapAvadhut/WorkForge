package com.avadhoot.workforge.issue;

import com.avadhoot.workforge.common.dto.ApiResponse;
import com.avadhoot.workforge.common.dto.PageResponse;
import com.avadhoot.workforge.issue.dto.ActivityResponse;
import com.avadhoot.workforge.issue.dto.CommentResponse;
import com.avadhoot.workforge.issue.dto.IssueDtos.AssigneePatch;
import com.avadhoot.workforge.issue.dto.IssueDtos.CommentRequest;
import com.avadhoot.workforge.issue.dto.IssueDtos.PriorityPatch;
import com.avadhoot.workforge.issue.dto.IssueDtos.SprintPatch;
import com.avadhoot.workforge.issue.dto.IssueDtos.StatusPatch;
import com.avadhoot.workforge.issue.dto.IssueDtos.UpdateIssueRequest;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Issue endpoints keyed by issue key (e.g. {@code MWS-1}) to match the frontend
 * contract. Issue creation and project-scoped listing also live under
 * {@code /api/v1/projects/{projectKey}/issues} (see ProjectIssueController).
 */
@RestController
@RequestMapping("/api/v1/issues")
public class IssueController {

    private final IssueService issueService;
    private final CommentService commentService;
    private final WatcherService watcherService;
    private final IssueActivityService issueActivityService;

    public IssueController(IssueService issueService, CommentService commentService,
                           WatcherService watcherService, IssueActivityService issueActivityService) {
        this.issueService = issueService;
        this.commentService = commentService;
        this.watcherService = watcherService;
        this.issueActivityService = issueActivityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ISSUE_CREATE')")
    public ApiResponse<IssueResponse> create(@Valid @RequestBody com.avadhoot.workforge.issue.dto.IssueDtos.CreateIssueRequest request) {
        return ApiResponse.success(issueService.create(request), "Issue created");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<PageResponse<IssueResponse>> list(
            @RequestParam(required = false) String projectKey,
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

    @GetMapping("/my-work")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<PageResponse<IssueResponse>> myWork(Pageable pageable) {
        return ApiResponse.success(issueService.myWork(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<PageResponse<IssueResponse>> search(
            @RequestParam(required = false) String jql,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assignee,
            Pageable pageable) {
        if (jql != null && !jql.isBlank()) {
            return ApiResponse.success(issueService.search(jql, pageable));
        }
        return ApiResponse.success(issueService.list(project, null, status, null, priority,
                assignee, null, null, pageable));
    }

    @GetMapping("/{issueKey}")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<IssueResponse> get(@PathVariable String issueKey) {
        return ApiResponse.success(issueService.getByKey(issueKey));
    }

    @GetMapping("/{issueKey}/subtasks")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<List<IssueResponse>> subtasks(@PathVariable String issueKey) {
        return ApiResponse.success(issueService.subtasksByKey(issueKey));
    }

    @PutMapping("/{issueKey}")
    @PreAuthorize("hasAuthority('ISSUE_UPDATE')")
    public ApiResponse<IssueResponse> updatePut(@PathVariable String issueKey,
                                                @Valid @RequestBody UpdateIssueRequest request) {
        return ApiResponse.success(issueService.update(issueKey, request), "Issue updated");
    }

    @PatchMapping("/{issueKey}")
    @PreAuthorize("hasAuthority('ISSUE_UPDATE')")
    public ApiResponse<IssueResponse> update(@PathVariable String issueKey,
                                             @Valid @RequestBody UpdateIssueRequest request) {
        return ApiResponse.success(issueService.update(issueKey, request), "Issue updated");
    }

    @PatchMapping("/{issueKey}/assignee")
    @PreAuthorize("hasAuthority('ISSUE_ASSIGN')")
    public ApiResponse<IssueResponse> assign(@PathVariable String issueKey, @RequestBody AssigneePatch patch) {
        return ApiResponse.success(issueService.assign(issueKey, patch.assigneeId()), "Assignee updated");
    }

    @PatchMapping("/{issueKey}/status")
    @PreAuthorize("hasAuthority('ISSUE_TRANSITION')")
    public ApiResponse<IssueResponse> transition(@PathVariable String issueKey,
                                                 @Valid @RequestBody StatusPatch patch) {
        return ApiResponse.success(
                issueService.transition(issueKey, com.avadhoot.workforge.common.CodeMappings.parseId(patch.statusId())),
                "Status updated");
    }

    @PatchMapping("/{issueKey}/priority")
    @PreAuthorize("hasAuthority('ISSUE_UPDATE')")
    public ApiResponse<IssueResponse> changePriority(@PathVariable String issueKey,
                                                     @RequestBody PriorityPatch patch) {
        return ApiResponse.success(issueService.changePriority(issueKey, patch.resolved()), "Priority updated");
    }

    @PatchMapping("/{issueKey}/sprint")
    @PreAuthorize("hasAuthority('ISSUE_UPDATE')")
    public ApiResponse<IssueResponse> changeSprint(@PathVariable String issueKey, @RequestBody SprintPatch patch) {
        return ApiResponse.success(issueService.changeSprint(issueKey, patch.sprintId()), "Sprint updated");
    }

    @PostMapping("/{issueKey}/labels/{labelId}")
    @PreAuthorize("hasAuthority('ISSUE_UPDATE')")
    public ApiResponse<IssueResponse> addLabel(@PathVariable String issueKey, @PathVariable Long labelId) {
        return ApiResponse.success(issueService.addLabel(issueKey, labelId), "Label added");
    }

    @DeleteMapping("/{issueKey}/labels/{labelId}")
    @PreAuthorize("hasAuthority('ISSUE_UPDATE')")
    public ApiResponse<IssueResponse> removeLabel(@PathVariable String issueKey, @PathVariable Long labelId) {
        return ApiResponse.success(issueService.removeLabel(issueKey, labelId), "Label removed");
    }

    @PostMapping("/{issueKey}/components/{componentId}")
    @PreAuthorize("hasAuthority('ISSUE_UPDATE')")
    public ApiResponse<IssueResponse> addComponent(@PathVariable String issueKey, @PathVariable Long componentId) {
        return ApiResponse.success(issueService.addComponent(issueKey, componentId), "Component added");
    }

    @DeleteMapping("/{issueKey}/components/{componentId}")
    @PreAuthorize("hasAuthority('ISSUE_UPDATE')")
    public ApiResponse<IssueResponse> removeComponent(@PathVariable String issueKey, @PathVariable Long componentId) {
        return ApiResponse.success(issueService.removeComponent(issueKey, componentId), "Component removed");
    }

    @DeleteMapping("/{issueKey}")
    @PreAuthorize("hasAuthority('ISSUE_DELETE')")
    public ApiResponse<Void> delete(@PathVariable String issueKey) {
        issueService.delete(issueKey);
        return ApiResponse.message("Issue deleted");
    }

    // ---- Watchers ----

    @PostMapping("/{issueKey}/watch")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<Void> watch(@PathVariable String issueKey) {
        watcherService.watch(issueService.findByKey(issueKey).getId(), SecurityUtils.requireCurrentUserId());
        return ApiResponse.message("Watching issue");
    }

    @DeleteMapping("/{issueKey}/watch")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<Void> unwatch(@PathVariable String issueKey) {
        watcherService.unwatch(issueService.findByKey(issueKey).getId(), SecurityUtils.requireCurrentUserId());
        return ApiResponse.message("Stopped watching issue");
    }

    @GetMapping("/{issueKey}/watchers")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<List<Long>> watchers(@PathVariable String issueKey) {
        return ApiResponse.success(watcherService.watchers(issueService.findByKey(issueKey).getId()));
    }

    // ---- Comments ----

    @PostMapping("/{issueKey}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('COMMENT_CREATE')")
    public ApiResponse<CommentResponse> addComment(@PathVariable String issueKey,
                                                   @Valid @RequestBody CommentRequest request) {
        return ApiResponse.success(commentService.add(issueKey, request.body()), "Comment added");
    }

    @GetMapping("/{issueKey}/comments")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<List<CommentResponse>> comments(@PathVariable String issueKey) {
        return ApiResponse.success(commentService.list(issueKey));
    }

    @PutMapping("/{issueKey}/comments/{commentId}")
    @PreAuthorize("hasAuthority('COMMENT_CREATE')")
    public ApiResponse<CommentResponse> updateCommentPut(@PathVariable String issueKey,
                                                         @PathVariable Long commentId,
                                                         @Valid @RequestBody CommentRequest request) {
        return ApiResponse.success(commentService.update(issueKey, commentId, request.body()), "Comment updated");
    }

    @PatchMapping("/{issueKey}/comments/{commentId}")
    @PreAuthorize("hasAuthority('COMMENT_CREATE')")
    public ApiResponse<CommentResponse> updateComment(@PathVariable String issueKey,
                                                      @PathVariable Long commentId,
                                                      @Valid @RequestBody CommentRequest request) {
        return ApiResponse.success(commentService.update(issueKey, commentId, request.body()), "Comment updated");
    }

    @DeleteMapping("/{issueKey}/comments/{commentId}")
    @PreAuthorize("hasAuthority('COMMENT_CREATE')")
    public ApiResponse<Void> deleteComment(@PathVariable String issueKey, @PathVariable Long commentId) {
        commentService.delete(issueKey, commentId);
        return ApiResponse.message("Comment deleted");
    }

    // ---- Activity (basic feed placeholder) ----

    @GetMapping("/{issueKey}/activity")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<List<ActivityResponse>> activity(@PathVariable String issueKey) {
        return ApiResponse.success(issueActivityService.activity(issueKey));
    }
}
