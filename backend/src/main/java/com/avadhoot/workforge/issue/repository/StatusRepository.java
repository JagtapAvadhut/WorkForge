package com.avadhoot.workforge.issue.repository;

import com.avadhoot.workforge.issue.domain.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusRepository extends JpaRepository<Status, Long> {

    Optional<Status> findByNameIgnoreCase(String name);
}
