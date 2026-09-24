package com.avadhoot.workforge.filter.domain;

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
@Table(name = "saved_filters")
public class SavedFilter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 150)
    private String name;

    /** Simplified query string, e.g. {@code project = ENG AND status = "In Progress"}. */
    @Column(name = "query", nullable = false, length = 2000)
    private String query;

    @Column(nullable = false)
    private boolean shared = false;
}
