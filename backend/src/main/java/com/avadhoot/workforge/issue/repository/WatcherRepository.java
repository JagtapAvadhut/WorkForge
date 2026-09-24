package com.avadhoot.workforge.issue.repository;

import com.avadhoot.workforge.issue.domain.Watcher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatcherRepository extends JpaRepository<Watcher, Long> {

    List<Watcher> findByIssueId(Long issueId);

    Optional<Watcher> findByIssueIdAndUserId(Long issueId, Long userId);

    boolean existsByIssueIdAndUserId(Long issueId, Long userId);
}
