package com.avadhoot.workforge.issue.repository;

import com.avadhoot.workforge.issue.domain.IssueType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IssueTypeRepository extends JpaRepository<IssueType, Long> {

    Optional<IssueType> findByNameIgnoreCase(String name);
}
