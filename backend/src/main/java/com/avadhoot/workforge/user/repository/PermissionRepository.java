package com.avadhoot.workforge.user.repository;

import com.avadhoot.workforge.user.domain.Permission;
import com.avadhoot.workforge.user.domain.PermissionName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(PermissionName name);
}
