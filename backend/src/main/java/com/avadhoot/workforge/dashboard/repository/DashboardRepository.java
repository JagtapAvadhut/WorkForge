package com.avadhoot.workforge.dashboard.repository;

import com.avadhoot.workforge.dashboard.domain.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DashboardRepository extends JpaRepository<Dashboard, Long> {

    @Query("select d from Dashboard d where d.ownerId = :userId or d.shared = true")
    List<Dashboard> findVisibleTo(@Param("userId") Long userId);
}
