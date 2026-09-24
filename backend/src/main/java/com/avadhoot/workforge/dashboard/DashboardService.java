package com.avadhoot.workforge.dashboard;

import com.avadhoot.workforge.dashboard.domain.Dashboard;
import com.avadhoot.workforge.dashboard.domain.DashboardGadget;
import com.avadhoot.workforge.dashboard.dto.DashboardDtos.AddGadgetRequest;
import com.avadhoot.workforge.dashboard.dto.DashboardDtos.CreateDashboardRequest;
import com.avadhoot.workforge.dashboard.dto.DashboardDtos.DashboardResponse;
import com.avadhoot.workforge.dashboard.dto.DashboardDtos.GadgetResponse;
import com.avadhoot.workforge.dashboard.repository.DashboardGadgetRepository;
import com.avadhoot.workforge.dashboard.repository.DashboardRepository;
import com.avadhoot.workforge.exception.BusinessException;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final DashboardGadgetRepository gadgetRepository;

    public DashboardService(DashboardRepository dashboardRepository,
                            DashboardGadgetRepository gadgetRepository) {
        this.dashboardRepository = dashboardRepository;
        this.gadgetRepository = gadgetRepository;
    }

    @Transactional
    public DashboardResponse create(CreateDashboardRequest request) {
        Dashboard dashboard = new Dashboard();
        dashboard.setOwnerId(SecurityUtils.requireCurrentUserId());
        dashboard.setName(request.name());
        dashboard.setShared(request.shared());
        return toResponse(dashboardRepository.save(dashboard));
    }

    @Transactional(readOnly = true)
    public List<DashboardResponse> list() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return dashboardRepository.findVisibleTo(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public GadgetResponse addGadget(Long dashboardId, AddGadgetRequest request) {
        Dashboard dashboard = findById(dashboardId);
        requireOwner(dashboard);
        DashboardGadget gadget = new DashboardGadget();
        gadget.setDashboardId(dashboardId);
        gadget.setType(request.type());
        gadget.setConfig(request.config());
        gadget.setPosition(request.position());
        return GadgetResponse.from(gadgetRepository.save(gadget));
    }

    @Transactional
    public void removeGadget(Long dashboardId, Long gadgetId) {
        Dashboard dashboard = findById(dashboardId);
        requireOwner(dashboard);
        DashboardGadget gadget = gadgetRepository.findById(gadgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Gadget", gadgetId));
        gadgetRepository.delete(gadget);
    }

    @Transactional
    public void delete(Long id) {
        Dashboard dashboard = findById(id);
        requireOwner(dashboard);
        gadgetRepository.findByDashboardIdOrderByPosition(id).forEach(gadgetRepository::delete);
        dashboardRepository.delete(dashboard);
    }

    private DashboardResponse toResponse(Dashboard dashboard) {
        List<GadgetResponse> gadgets = gadgetRepository
                .findByDashboardIdOrderByPosition(dashboard.getId()).stream()
                .map(GadgetResponse::from).toList();
        return DashboardResponse.from(dashboard, gadgets);
    }

    private Dashboard findById(Long id) {
        return dashboardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard", id));
    }

    private void requireOwner(Dashboard dashboard) {
        if (!dashboard.getOwnerId().equals(SecurityUtils.requireCurrentUserId())) {
            throw new BusinessException("You can only modify your own dashboards");
        }
    }
}
