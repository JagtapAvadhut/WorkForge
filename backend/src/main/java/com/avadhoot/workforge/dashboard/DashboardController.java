package com.avadhoot.workforge.dashboard;

import com.avadhoot.workforge.common.dto.ApiResponse;
import com.avadhoot.workforge.dashboard.dto.DashboardDtos.AddGadgetRequest;
import com.avadhoot.workforge.dashboard.dto.DashboardDtos.CreateDashboardRequest;
import com.avadhoot.workforge.dashboard.dto.DashboardDtos.DashboardResponse;
import com.avadhoot.workforge.dashboard.dto.DashboardDtos.GadgetResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('DASHBOARD_MANAGE')")
    public ApiResponse<DashboardResponse> create(@Valid @RequestBody CreateDashboardRequest request) {
        return ApiResponse.success(dashboardService.create(request), "Dashboard created");
    }

    @GetMapping
    public ApiResponse<List<DashboardResponse>> list() {
        return ApiResponse.success(dashboardService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<DashboardResponse> get(@PathVariable Long id) {
        return ApiResponse.success(dashboardService.get(id));
    }

    @PostMapping("/{id}/gadgets")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('DASHBOARD_MANAGE')")
    public ApiResponse<GadgetResponse> addGadget(@PathVariable Long id,
                                                 @Valid @RequestBody AddGadgetRequest request) {
        return ApiResponse.success(dashboardService.addGadget(id, request), "Gadget added");
    }

    @DeleteMapping("/{id}/gadgets/{gadgetId}")
    @PreAuthorize("hasAuthority('DASHBOARD_MANAGE')")
    public ApiResponse<Void> removeGadget(@PathVariable Long id, @PathVariable Long gadgetId) {
        dashboardService.removeGadget(id, gadgetId);
        return ApiResponse.message("Gadget removed");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DASHBOARD_MANAGE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dashboardService.delete(id);
        return ApiResponse.message("Dashboard deleted");
    }
}
