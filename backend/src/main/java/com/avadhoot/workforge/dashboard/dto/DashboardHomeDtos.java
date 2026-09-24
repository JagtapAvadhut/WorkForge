package com.avadhoot.workforge.dashboard.dto;

import com.avadhoot.workforge.issue.domain.StatusCategory;
import com.avadhoot.workforge.issue.dto.ActivityResponse;
import com.avadhoot.workforge.issue.dto.IssueResponse;

import java.util.List;

/**
 * Home-dashboard payloads consumed by the React Dashboard page
 * ({@code /api/v1/dashboard/*}).
 */
public final class DashboardHomeDtos {

    private DashboardHomeDtos() {
    }

    public record DashboardStatsResponse(
            long openIssues,
            long assignedToMe,
            long reportedByMe,
            long dueSoon,
            List<StatusBucket> statusDistribution,
            List<PriorityBucket> priorityDistribution) {
    }

    public record StatusBucket(StatusCategory category, String label, long count) {
    }

    public record PriorityBucket(String priority, long count) {
    }

    public record DashboardHomeBundle(
            DashboardStatsResponse stats,
            List<IssueResponse> myOpenIssues,
            List<IssueResponse> assignedToMe,
            List<ActivityResponse> activity) {
    }
}
