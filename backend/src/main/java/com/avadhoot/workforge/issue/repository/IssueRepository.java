package com.avadhoot.workforge.issue.repository;

import com.avadhoot.workforge.issue.domain.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {

    Optional<Issue> findByIssueKey(String issueKey);

    List<Issue> findByParentId(Long parentId);

    List<Issue> findBySprintId(Long sprintId);

    List<Issue> findByProjectIdAndSprintIdIsNull(Long projectId);
}
