package com.avadhoot.workforge.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "workflow_statuses", uniqueConstraints = @UniqueConstraint(columnNames = {"workflow_id", "status_id"}))
public class WorkflowStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "is_initial", nullable = false)
    private boolean initial = false;

    @Column(name = "position", nullable = false)
    private int position;
}
