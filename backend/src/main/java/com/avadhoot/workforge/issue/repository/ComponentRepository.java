package com.avadhoot.workforge.issue.repository;

import com.avadhoot.workforge.issue.domain.Component;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComponentRepository extends JpaRepository<Component, Long> {

    List<Component> findByProjectId(Long projectId);

    boolean existsByProjectIdAndNameIgnoreCase(Long projectId, String name);
}
