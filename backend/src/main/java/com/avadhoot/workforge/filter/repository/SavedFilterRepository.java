package com.avadhoot.workforge.filter.repository;

import com.avadhoot.workforge.filter.domain.SavedFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SavedFilterRepository extends JpaRepository<SavedFilter, Long> {

    @Query("select f from SavedFilter f where f.ownerId = :userId or f.shared = true")
    List<SavedFilter> findVisibleTo(@Param("userId") Long userId);
}
