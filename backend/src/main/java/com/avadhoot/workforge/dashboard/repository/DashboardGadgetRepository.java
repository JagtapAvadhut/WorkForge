package com.avadhoot.workforge.dashboard.repository;

import com.avadhoot.workforge.dashboard.domain.DashboardGadget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DashboardGadgetRepository extends JpaRepository<DashboardGadget, Long> {

    List<DashboardGadget> findByDashboardIdOrderByPosition(Long dashboardId);
}
