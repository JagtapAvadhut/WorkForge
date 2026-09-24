package com.avadhoot.workforge.sprint;

import com.avadhoot.workforge.common.dto.ApiResponse;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.sprint.dto.SprintDtos.CompleteSprintRequest;
import com.avadhoot.workforge.sprint.dto.SprintDtos.CreateSprintRequest;
import com.avadhoot.workforge.sprint.dto.SprintDtos.SprintResponse;
import com.avadhoot.workforge.sprint.dto.SprintDtos.StartSprintRequest;
import com.avadhoot.workforge.sprint.dto.SprintDtos.UpdateSprintRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/v1/sprints")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SPRINT_MANAGE')")
    public ApiResponse<SprintResponse> create(@Valid @RequestBody CreateSprintRequest request) {
        return ApiResponse.success(sprintService.create(request), "Sprint created");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ApiResponse<List<SprintResponse>> list(@RequestParam Long projectId) {
        return ApiResponse.success(sprintService.list(projectId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SPRINT_MANAGE')")
    public ApiResponse<SprintResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody UpdateSprintRequest request) {
        return ApiResponse.success(sprintService.update(id, request), "Sprint updated");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('SPRINT_MANAGE')")
    public ApiResponse<SprintResponse> patch(@PathVariable Long id,
                                             @Valid @RequestBody UpdateSprintRequest request) {
        return ApiResponse.success(sprintService.update(id, request), "Sprint updated");
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAuthority('SPRINT_MANAGE')")
    public ApiResponse<SprintResponse> startPatch(@PathVariable Long id,
                                                  @RequestBody(required = false) StartSprintRequest request) {
        return ApiResponse.success(sprintService.start(id, request), "Sprint started");
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('SPRINT_MANAGE')")
    public ApiResponse<SprintResponse> start(@PathVariable Long id,
                                             @RequestBody(required = false) StartSprintRequest request) {
        return ApiResponse.success(sprintService.start(id, request), "Sprint started");
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('SPRINT_MANAGE')")
    public ApiResponse<SprintResponse> completePatch(@PathVariable Long id,
                                                     @RequestBody(required = false) CompleteSprintRequest request) {
        String moveTo = request != null ? request.moveToSprintId() : null;
        return ApiResponse.success(sprintService.complete(id, moveTo), "Sprint completed");
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('SPRINT_MANAGE')")
    public ApiResponse<SprintResponse> complete(@PathVariable Long id,
                                                @RequestBody(required = false) CompleteSprintRequest request) {
        String moveTo = request != null ? request.moveToSprintId() : null;
        return ApiResponse.success(sprintService.complete(id, moveTo), "Sprint completed");
    }

    @GetMapping("/{id}/issues")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<List<IssueResponse>> issues(@PathVariable Long id) {
        return ApiResponse.success(sprintService.issues(id));
    }

    @GetMapping("/backlog")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<List<IssueResponse>> backlog(@RequestParam Long projectId) {
        return ApiResponse.success(sprintService.backlog(projectId));
    }
}
