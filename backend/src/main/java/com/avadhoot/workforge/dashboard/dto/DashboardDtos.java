package com.avadhoot.workforge.dashboard.dto;

import com.avadhoot.workforge.dashboard.domain.Dashboard;
import com.avadhoot.workforge.dashboard.domain.DashboardGadget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record CreateDashboardRequest(
            @NotBlank @Size(max = 150) String name,
            boolean shared) {
    }

    public record AddGadgetRequest(
            @NotBlank @Size(max = 50) String type,
            String config,
            int position) {
    }

    public record GadgetResponse(Long id, String type, String config, int position) {
        public static GadgetResponse from(DashboardGadget g) {
            return new GadgetResponse(g.getId(), g.getType(), g.getConfig(), g.getPosition());
        }
    }

    public record DashboardResponse(Long id, Long ownerId, String name, boolean shared,
                                    List<GadgetResponse> gadgets) {
        public static DashboardResponse from(Dashboard d, List<GadgetResponse> gadgets) {
            return new DashboardResponse(d.getId(), d.getOwnerId(), d.getName(), d.isShared(), gadgets);
        }
    }
}
