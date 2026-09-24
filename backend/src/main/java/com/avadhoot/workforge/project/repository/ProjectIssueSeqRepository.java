package com.avadhoot.workforge.project.repository;

import com.avadhoot.workforge.project.domain.ProjectIssueSeq;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectIssueSeqRepository extends JpaRepository<ProjectIssueSeq, Long> {

    /**
     * Pessimistically locks the counter row (SELECT ... FOR UPDATE) so concurrent
     * issue creation cannot allocate duplicate sequence numbers.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ProjectIssueSeq s where s.projectId = :projectId")
    Optional<ProjectIssueSeq> findByProjectIdForUpdate(@Param("projectId") Long projectId);
}
