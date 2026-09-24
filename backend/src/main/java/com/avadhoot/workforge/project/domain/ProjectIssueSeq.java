package com.avadhoot.workforge.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Per-project monotonically increasing issue counter. A dedicated row is locked
 * with {@code SELECT ... FOR UPDATE} to generate collision-free issue keys.
 */
@Getter
@Setter
@Entity
@Table(name = "project_issue_seq")
public class ProjectIssueSeq {

    @Id
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "seq", nullable = false)
    private long seq;
}
