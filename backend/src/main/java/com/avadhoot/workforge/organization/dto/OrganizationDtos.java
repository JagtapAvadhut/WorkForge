package com.avadhoot.workforge.organization.dto;

import com.avadhoot.workforge.organization.domain.Organization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class OrganizationDtos {

    private OrganizationDtos() {
    }

    public record CreateOrganizationRequest(
            @NotBlank @Size(max = 50) String key,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 500) String description) {
    }

    public record UpdateOrganizationRequest(
            @Size(max = 150) String name,
            @Size(max = 500) String description,
            Boolean enabled) {
    }

    public record OrganizationResponse(
            Long id, String key, String name, String description, boolean enabled) {
        public static OrganizationResponse from(Organization o) {
            return new OrganizationResponse(o.getId(), o.getKey(), o.getName(),
                    o.getDescription(), o.isEnabled());
        }
    }
}
