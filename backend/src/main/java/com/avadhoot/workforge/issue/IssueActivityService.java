package com.avadhoot.workforge.issue;

import com.avadhoot.workforge.audit.domain.AuditLog;
import com.avadhoot.workforge.audit.repository.AuditLogRepository;
import com.avadhoot.workforge.issue.domain.Issue;
import com.avadhoot.workforge.issue.dto.ActivityResponse;
import com.avadhoot.workforge.user.dto.UserResponse;
import com.avadhoot.workforge.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IssueActivityService {

    private final IssueService issueService;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public IssueActivityService(IssueService issueService, AuditLogRepository auditLogRepository,
                                UserRepository userRepository) {
        this.issueService = issueService;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> activity(String issueKey) {
        Issue issue = issueService.findByKey(issueKey);
        return auditLogRepository
                .findByEntityTypeAndEntityId(
                        "ISSUE",
                        issue.getId(),
                        PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(this::toActivity)
                .toList();
    }

    private ActivityResponse toActivity(AuditLog log) {
        UserResponse actor = log.getActorId() == null ? null
                : userRepository.findById(log.getActorId()).map(UserResponse::from).orElse(null);
        String details = log.getDetails() == null ? "" : log.getDetails();
        String from = null;
        String to = null;
        int arrow = details.indexOf("->");
        if (arrow >= 0) {
            from = details.substring(0, arrow).trim();
            to = details.substring(arrow + 2).trim();
        }
        return new ActivityResponse(
                String.valueOf(log.getId()),
                mapAction(log.getAction()),
                actor,
                fieldFor(log.getAction()),
                from,
                to != null ? to : details,
                log.getCreatedAt());
    }

    private static String mapAction(String action) {
        if (action == null) {
            return "UPDATED";
        }
        return switch (action) {
            case "ISSUE_CREATED", "CREATED" -> "CREATED";
            case "ISSUE_STATUS_CHANGED", "STATUS_CHANGED" -> "STATUS_CHANGED";
            case "ISSUE_ASSIGNED", "ASSIGNED" -> "ASSIGNED";
            case "ISSUE_PRIORITY_CHANGED", "PRIORITY_CHANGED" -> "PRIORITY_CHANGED";
            case "COMMENT_ADDED", "COMMENTED" -> "COMMENTED";
            case "ISSUE_SPRINT_CHANGED", "SPRINT_CHANGED" -> "SPRINT_CHANGED";
            default -> "UPDATED";
        };
    }

    private static String fieldFor(String action) {
        if (action == null) {
            return null;
        }
        return switch (action) {
            case "ISSUE_STATUS_CHANGED", "STATUS_CHANGED" -> "status";
            case "ISSUE_ASSIGNED", "ASSIGNED" -> "assignee";
            case "ISSUE_PRIORITY_CHANGED", "PRIORITY_CHANGED" -> "priority";
            case "ISSUE_SPRINT_CHANGED", "SPRINT_CHANGED" -> "sprint";
            default -> null;
        };
    }
}
