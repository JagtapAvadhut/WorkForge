package com.avadhoot.workforge.audit;

import com.avadhoot.workforge.audit.domain.AuditLog;
import com.avadhoot.workforge.audit.repository.AuditLogRepository;
import com.avadhoot.workforge.common.dto.PageResponse;
import com.avadhoot.workforge.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(String entityType, Long entityId, String action, String details) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setActorId(SecurityUtils.currentUserId().orElse(null));
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLog> history(String entityType, Long entityId, Pageable pageable) {
        return PageResponse.of(
                auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable));
    }
}
