package com.avadhoot.workforge.audit.event;

/**
 * Domain events published via ApplicationEventPublisher when issues change.
 * Consumed by {@link DomainEventListener} for audit logging and notifications.
 */
public final class IssueEvents {

    private IssueEvents() {
    }

    public record IssueCreatedEvent(Long issueId, String issueKey, Long actorId, Long assigneeId) {
    }

    public record IssueUpdatedEvent(Long issueId, String issueKey, Long actorId, String changeSummary) {
    }

    public record IssueAssignedEvent(Long issueId, String issueKey, Long actorId, Long assigneeId) {
    }

    public record IssueTransitionedEvent(Long issueId, String issueKey, Long actorId,
                                         String fromStatus, String toStatus) {
    }

    public record CommentCreatedEvent(Long issueId, String issueKey, Long commentId, Long actorId) {
    }
}
