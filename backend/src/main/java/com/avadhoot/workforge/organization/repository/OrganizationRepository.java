package com.avadhoot.workforge.organization.repository;

import com.avadhoot.workforge.organization.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByKey(String key);

    boolean existsByKey(String key);
}
