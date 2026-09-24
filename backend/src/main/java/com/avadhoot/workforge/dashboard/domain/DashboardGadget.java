package com.avadhoot.workforge.dashboard.domain;

import com.avadhoot.workforge.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dashboard_gadgets")
public class DashboardGadget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dashboard_id", nullable = false)
    private Long dashboardId;

    /** Gadget type, e.g. ASSIGNED_TO_ME, SPRINT_BURNDOWN, FILTER_RESULTS. */
    @Column(name = "gadget_type", nullable = false, length = 50)
    private String type;

    /** Opaque JSON configuration string. */
    @Column(name = "config", columnDefinition = "text")
    private String config;

    @Column(name = "position", nullable = false)
    private int position;
}
