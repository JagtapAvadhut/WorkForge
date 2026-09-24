package com.avadhoot.workforge.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/**
 * Base entity that additionally carries an optimistic-locking version column.
 */
@MappedSuperclass
public abstract class AuditableEntity extends BaseEntity {

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Long getVersion() {
        return version;
    }
}
