package com.avadhoot.workforge.dashboard;

import com.avadhoot.workforge.audit.domain.AuditLog;
import com.avadhoot.workforge.audit.repository.AuditLogRepository;
import com.avadhoot.workforge.common.CodeMappings;
import com.avadhoot.workforge.dashboard.dto.DashboardHomeDtos.DashboardStatsResponse;
import com.avadhoot.workforge.dashboard.dto.DashboardHomeDtos.PriorityBucket;
import com.avadhoot.workforge.dashboard.dto.DashboardHomeDtos.StatusBucket;
import com.avadhoot.workforge.issue.IssueMapper;
import com.avadhoot.workforge.issue.domain.Issue;
import com.avadhoot.workforge.issue.domain.StatusCategory;
import com.avadhoot.workforge.issue.dto.ActivityResponse;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.issue.repository.IssueRepository;
import com.avadhoot.workforge.issue.search.IssueSpecifications;
import com.avadhoot.workforge.security.SecurityUtils;
import com.avadhoot.workforge.user.dto.UserResponse;
import com.avadhoot.workforge.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardHomeService {

    private static final int LIST_LIMIT = 10;
    private static final int ACTIVITY_LIMIT = 20;

    private final IssueRepository issueRepository;
    private final IssueMapper issueMapper;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public DashboardHomeService(IssueRepository issueRepository, IssueMapper issueMapper,
                                AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.issueMapper = issueMapper;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse stats() {
        Long userId = SecurityUtils.requireCurrentUserId();
        LocalDate soon = LocalDate.now().plusDays(7);

        long openIssues = issueRepository.count(notDone());
        long assignedToMe = issueRepository.count(assignedTo(userId).and(notDone()));
        long reportedByMe = issueRepository.count(reportedBy(userId));
        long dueSoon = issueRepository.count(notDone().and((root, q, cb) -> cb.and(
                cb.isNotNull(root.get("dueDate")),
                cb.lessThanOrEqualTo(root.get("dueDate"), soon),
                cb.greaterThanOrEqualTo(root.get("dueDate"), LocalDate.now())
        )));

        Map<StatusCategory, Long> statusCounts = new EnumMap<>(StatusCategory.class);
        for (StatusCategory category : StatusCategory.values()) {
            statusCounts.put(category, 0L);
        }
        Map<String, Long> priorityCounts = new HashMap<>();

        // Aggregate across a bounded page of recent issues to avoid loading the entire table.
        List<Issue> sample = issueRepository.findAll(
                PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "updatedAt"))).getContent();
        for (Issue issue : sample) {
            if (issue.getStatus() != null && issue.getStatus().getCategory() != null) {
                statusCounts.merge(issue.getStatus().getCategory(), 1L, Long::sum);
            }
            if (issue.getPriority() != null) {
                String code = CodeMappings.priorityCode(issue.getPriority().getName());
                priorityCounts.merge(code, 1L, Long::sum);
            }
        }

        // Prefer accurate open counts for status distribution.
        List<StatusBucket> statusDistribution = new ArrayList<>();
        statusDistribution.add(new StatusBucket(StatusCategory.TODO, "To Do",
                issueRepository.count(byCategory(StatusCategory.TODO))));
        statusDistribution.add(new StatusBucket(StatusCategory.IN_PROGRESS, "In Progress",
                issueRepository.count(byCategory(StatusCategory.IN_PROGRESS))));
        statusDistribution.add(new StatusBucket(StatusCategory.DONE, "Done",
                issueRepository.count(byCategory(StatusCategory.DONE))));

        List<PriorityBucket> priorityDistribution = priorityCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new PriorityBucket(e.getKey(), e.getValue()))
                .toList();

        return new DashboardStatsResponse(
                openIssues, assignedToMe, reportedByMe, dueSoon,
                statusDistribution, priorityDistribution);
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> myOpenIssues() {
        Long userId = SecurityUtils.requireCurrentUserId();
        Specification<Issue> spec = reportedBy(userId).and(notDone());
        return issueRepository.findAll(spec,
                        PageRequest.of(0, LIST_LIMIT, Sort.by(Sort.Direction.DESC, "updatedAt")))
                .stream().map(issueMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> assignedToMe() {
        Long userId = SecurityUtils.requireCurrentUserId();
        Specification<Issue> spec = assignedTo(userId).and(notDone());
        return issueRepository.findAll(spec,
                        PageRequest.of(0, LIST_LIMIT, Sort.by(Sort.Direction.DESC, "updatedAt")))
                .stream().map(issueMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> recentActivity() {
        return auditLogRepository.findAll(
                        PageRequest.of(0, ACTIVITY_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")))
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
        String action = log.getAction() == null ? "UPDATED" : log.getAction();
        String type = switch (action) {
            case "ISSUE_CREATED", "CREATED" -> "CREATED";
            case "ISSUE_STATUS_CHANGED", "STATUS_CHANGED" -> "STATUS_CHANGED";
            case "ISSUE_ASSIGNED", "ASSIGNED" -> "ASSIGNED";
            case "ISSUE_PRIORITY_CHANGED", "PRIORITY_CHANGED" -> "PRIORITY_CHANGED";
            case "COMMENT_ADDED", "COMMENTED" -> "COMMENTED";
            case "ISSUE_SPRINT_CHANGED", "SPRINT_CHANGED" -> "SPRINT_CHANGED";
            default -> "UPDATED";
        };
        return new ActivityResponse(
                String.valueOf(log.getId()),
                type,
                actor,
                null,
                from,
                to != null ? to : details,
                log.getCreatedAt());
    }

    private static Specification<Issue> assignedTo(Long userId) {
        return IssueSpecifications.assigneeId(userId);
    }

    private static Specification<Issue> reportedBy(Long userId) {
        return (root, q, cb) -> cb.equal(root.get("reporterId"), userId);
    }

    private static Specification<Issue> notDone() {
        return (root, q, cb) -> cb.notEqual(root.get("status").get("category"), StatusCategory.DONE);
    }

    private static Specification<Issue> byCategory(StatusCategory category) {
        return (root, q, cb) -> cb.equal(root.get("status").get("category"), category);
    }
}
