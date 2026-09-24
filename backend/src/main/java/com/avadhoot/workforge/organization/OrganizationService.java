package com.avadhoot.workforge.organization;

import com.avadhoot.workforge.common.dto.PageResponse;
import com.avadhoot.workforge.exception.DuplicateResourceException;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.organization.domain.Organization;
import com.avadhoot.workforge.organization.dto.OrganizationDtos.CreateOrganizationRequest;
import com.avadhoot.workforge.organization.dto.OrganizationDtos.OrganizationResponse;
import com.avadhoot.workforge.organization.dto.OrganizationDtos.UpdateOrganizationRequest;
import com.avadhoot.workforge.organization.repository.OrganizationRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request) {
        if (organizationRepository.existsByKey(request.key())) {
            throw new DuplicateResourceException("Organization key already exists: " + request.key());
        }
        Organization org = new Organization();
        org.setKey(request.key());
        org.setName(request.name());
        org.setDescription(request.description());
        return OrganizationResponse.from(organizationRepository.save(org));
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(Long id) {
        return OrganizationResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrganizationResponse> list(Pageable pageable) {
        return PageResponse.of(organizationRepository.findAll(pageable).map(OrganizationResponse::from));
    }

    @Transactional
    public OrganizationResponse update(Long id, UpdateOrganizationRequest request) {
        Organization org = findById(id);
        if (request.name() != null) {
            org.setName(request.name());
        }
        if (request.description() != null) {
            org.setDescription(request.description());
        }
        if (request.enabled() != null) {
            org.setEnabled(request.enabled());
        }
        return OrganizationResponse.from(org);
    }

    @Transactional
    public void delete(Long id) {
        Organization org = findById(id);
        org.setEnabled(false);
    }

    private Organization findById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", id));
    }
}
