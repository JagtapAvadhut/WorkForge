package com.avadhoot.workforge.issue.repository;

import com.avadhoot.workforge.issue.domain.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriorityRepository extends JpaRepository<Priority, Long> {

    Optional<Priority> findByNameIgnoreCase(String name);
}
