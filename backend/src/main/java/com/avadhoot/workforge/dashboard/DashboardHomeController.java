package com.avadhoot.workforge.dashboard;

import com.avadhoot.workforge.common.dto.ApiResponse;
import com.avadhoot.workforge.dashboard.dto.DashboardHomeDtos.DashboardStatsResponse;
import com.avadhoot.workforge.issue.dto.ActivityResponse;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Home dashboard summary endpoints used by the React Dashboard page.
 * Distinct from {@link DashboardController} which manages saved dashboard gadgets.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardHomeController {

    private final DashboardHomeService dashboardHomeService;

    public DashboardHomeController(DashboardHomeService dashboardHomeService) {
        this.dashboardHomeService = dashboardHomeService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<DashboardStatsResponse> stats() {
        return ApiResponse.success(dashboardHomeService.stats());
    }

    @GetMapping("/my-open-issues")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<List<IssueResponse>> myOpenIssues() {
        return ApiResponse.success(dashboardHomeService.myOpenIssues());
    }

    @GetMapping("/assigned-to-me")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<List<IssueResponse>> assignedToMe() {
        return ApiResponse.success(dashboardHomeService.assignedToMe());
    }

    @GetMapping("/activity")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<List<ActivityResponse>> activity() {
        return ApiResponse.success(dashboardHomeService.recentActivity());
    }
}
