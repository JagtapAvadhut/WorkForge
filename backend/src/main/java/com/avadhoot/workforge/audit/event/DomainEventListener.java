package com.avadhoot.workforge.audit.event;

import com.avadhoot.workforge.audit.AuditService;
import com.avadhoot.workforge.audit.event.IssueEvents.CommentCreatedEvent;
import com.avadhoot.workforge.audit.event.IssueEvents.IssueAssignedEvent;
import com.avadhoot.workforge.audit.event.IssueEvents.IssueCreatedEvent;
import com.avadhoot.workforge.audit.event.IssueEvents.IssueTransitionedEvent;
import com.avadhoot.workforge.audit.event.IssueEvents.IssueUpdatedEvent;
import com.avadhoot.workforge.notification.NotificationService;
import com.avadhoot.workforge.notification.domain.NotificationType;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists audit records and fans out notifications in response to domain events.
 */
@Component
public class DomainEventListener {

    private final AuditService auditService;
    private final NotificationService notificationService;

    public DomainEventListener(AuditService auditService, NotificationService notificationService) {
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCreated(IssueCreatedEvent event) {
        auditService.record("ISSUE", event.issueId(), "CREATED",
                "Issue %s created".formatted(event.issueKey()));
        if (event.assigneeId() != null) {
            notificationService.notify(event.assigneeId(), NotificationType.ISSUE_ASSIGNED,
                    "Assigned " + event.issueKey(),
                    "You were assigned issue " + event.issueKey(), "ISSUE", event.issueId());
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onUpdated(IssueUpdatedEvent event) {
        auditService.record("ISSUE", event.issueId(), "UPDATED", event.changeSummary());
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAssigned(IssueAssignedEvent event) {
        auditService.record("ISSUE", event.issueId(), "ASSIGNED",
                "Assignee set to user " + event.assigneeId());
        if (event.assigneeId() != null) {
            notificationService.notify(event.assigneeId(), NotificationType.ISSUE_ASSIGNED,
                    "Assigned " + event.issueKey(),
                    "You were assigned issue " + event.issueKey(), "ISSUE", event.issueId());
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransitioned(IssueTransitionedEvent event) {
        auditService.record("ISSUE", event.issueId(), "TRANSITIONED",
                "%s -> %s".formatted(event.fromStatus(), event.toStatus()));
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCommented(CommentCreatedEvent event) {
        auditService.record("ISSUE", event.issueId(), "COMMENTED",
                "Comment %d added".formatted(event.commentId()));
    }
}
