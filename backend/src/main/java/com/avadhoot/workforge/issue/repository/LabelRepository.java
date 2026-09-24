package com.avadhoot.workforge.issue.repository;

import com.avadhoot.workforge.issue.domain.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabelRepository extends JpaRepository<Label, Long> {

    List<Label> findByProjectId(Long projectId);

    Optional<Label> findByProjectIdAndNameIgnoreCase(Long projectId, String name);
}
