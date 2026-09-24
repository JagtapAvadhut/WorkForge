package com.avadhoot.workforge.organization;

import com.avadhoot.workforge.common.dto.ApiResponse;
import com.avadhoot.workforge.common.dto.PageResponse;
import com.avadhoot.workforge.organization.dto.OrganizationDtos.CreateOrganizationRequest;
import com.avadhoot.workforge.organization.dto.OrganizationDtos.OrganizationResponse;
import com.avadhoot.workforge.organization.dto.OrganizationDtos.UpdateOrganizationRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ORG_MANAGE')")
    public ApiResponse<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
        return ApiResponse.success(organizationService.create(request), "Organization created");
    }

    @GetMapping
    public ApiResponse<PageResponse<OrganizationResponse>> list(Pageable pageable) {
        return ApiResponse.success(organizationService.list(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrganizationResponse> get(@PathVariable Long id) {
        return ApiResponse.success(organizationService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG_MANAGE')")
    public ApiResponse<OrganizationResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateOrganizationRequest request) {
        return ApiResponse.success(organizationService.update(id, request), "Organization updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG_MANAGE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        organizationService.delete(id);
        return ApiResponse.message("Organization disabled");
    }
}
