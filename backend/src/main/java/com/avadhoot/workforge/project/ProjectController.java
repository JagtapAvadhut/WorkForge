package com.avadhoot.workforge.project;

import com.avadhoot.workforge.common.dto.ApiResponse;
import com.avadhoot.workforge.common.dto.PageResponse;
import com.avadhoot.workforge.project.dto.ProjectDtos.AddMemberRequest;
import com.avadhoot.workforge.project.dto.ProjectDtos.CreateProjectRequest;
import com.avadhoot.workforge.project.dto.ProjectDtos.ProjectMemberResponse;
import com.avadhoot.workforge.project.dto.ProjectDtos.ProjectResponse;
import com.avadhoot.workforge.project.dto.ProjectDtos.UpdateProjectRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public ApiResponse<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        return ApiResponse.success(projectService.create(request), "Project created");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ApiResponse<PageResponse<ProjectResponse>> list(Pageable pageable) {
        return ApiResponse.success(projectService.list(pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ApiResponse<List<ProjectResponse>> listAll() {
        return ApiResponse.success(projectService.listAll());
    }

    @GetMapping("/{projectKey}")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ApiResponse<ProjectResponse> get(@PathVariable String projectKey) {
        return ApiResponse.success(projectService.get(projectKey));
    }

    @PutMapping("/{projectKey}")
    @PreAuthorize("hasAuthority('PROJECT_MANAGE')")
    public ApiResponse<ProjectResponse> update(@PathVariable String projectKey,
                                               @Valid @RequestBody UpdateProjectRequest request) {
        return ApiResponse.success(projectService.update(projectKey, request), "Project updated");
    }

    @PatchMapping("/{projectKey}")
    @PreAuthorize("hasAuthority('PROJECT_MANAGE')")
    public ApiResponse<ProjectResponse> patch(@PathVariable String projectKey,
                                              @Valid @RequestBody UpdateProjectRequest request) {
        return ApiResponse.success(projectService.update(projectKey, request), "Project updated");
    }

    @DeleteMapping("/{projectKey}")
    @PreAuthorize("hasAuthority('PROJECT_MANAGE')")
    public ApiResponse<Void> delete(@PathVariable String projectKey) {
        projectService.delete(projectKey);
        return ApiResponse.message("Project disabled");
    }

    @PostMapping("/{projectKey}/members")
    @PreAuthorize("hasAuthority('PROJECT_MANAGE')")
    public ApiResponse<ProjectMemberResponse> addMember(@PathVariable String projectKey,
                                                        @Valid @RequestBody AddMemberRequest request) {
        return ApiResponse.success(projectService.addMember(projectKey, request), "Member added");
    }

    @GetMapping("/{projectKey}/members")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ApiResponse<List<ProjectMemberResponse>> members(@PathVariable String projectKey) {
        return ApiResponse.success(projectService.members(projectKey));
    }

    @DeleteMapping("/{projectKey}/members/{userId}")
    @PreAuthorize("hasAuthority('PROJECT_MANAGE')")
    public ApiResponse<Void> removeMember(@PathVariable String projectKey, @PathVariable String userId) {
        projectService.removeMember(projectKey, userId);
        return ApiResponse.message("Member removed");
    }
}
