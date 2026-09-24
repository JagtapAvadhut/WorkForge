package com.avadhoot.workforge.config;

import com.avadhoot.workforge.security.SecurityUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Supplies the current user id to JPA auditing (createdBy / updatedBy).
 */
@Component("auditorAware")
public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        return SecurityUtils.currentUserId();
    }
}
