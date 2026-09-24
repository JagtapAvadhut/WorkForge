package com.avadhoot.workforge.issue;

import com.avadhoot.workforge.common.dto.ApiResponse;
import com.avadhoot.workforge.issue.dto.LabelComponentDtos.ComponentResponse;
import com.avadhoot.workforge.issue.dto.LabelComponentDtos.CreateComponentRequest;
import com.avadhoot.workforge.issue.dto.LabelComponentDtos.CreateLabelRequest;
import com.avadhoot.workforge.issue.dto.LabelComponentDtos.LabelResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class LabelComponentController {

    private final LabelComponentService service;

    public LabelComponentController(LabelComponentService service) {
        this.service = service;
    }

    @PostMapping("/labels")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROJECT_MANAGE')")
    public ApiResponse<LabelResponse> createLabel(@Valid @RequestBody CreateLabelRequest request) {
        return ApiResponse.success(service.createLabel(request), "Label created");
    }

    @GetMapping("/labels")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ApiResponse<List<LabelResponse>> labels(@RequestParam Long projectId) {
        return ApiResponse.success(service.labels(projectId));
    }

    @DeleteMapping("/labels/{id}")
    @PreAuthorize("hasAuthority('PROJECT_MANAGE')")
    public ApiResponse<Void> deleteLabel(@PathVariable Long id) {
        service.deleteLabel(id);
        return ApiResponse.message("Label deleted");
    }

    @PostMapping("/components")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROJECT_MANAGE')")
    public ApiResponse<ComponentResponse> createComponent(@Valid @RequestBody CreateComponentRequest request) {
        return ApiResponse.success(service.createComponent(request), "Component created");
    }

    @GetMapping("/components")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    public ApiResponse<List<ComponentResponse>> components(@RequestParam Long projectId) {
        return ApiResponse.success(service.components(projectId));
    }

    @DeleteMapping("/components/{id}")
    @PreAuthorize("hasAuthority('PROJECT_MANAGE')")
    public ApiResponse<Void> deleteComponent(@PathVariable Long id) {
        service.deleteComponent(id);
        return ApiResponse.message("Component deleted");
    }
}
