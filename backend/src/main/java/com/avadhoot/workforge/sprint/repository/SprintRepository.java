package com.avadhoot.workforge.sprint.repository;

import com.avadhoot.workforge.sprint.domain.Sprint;
import com.avadhoot.workforge.sprint.domain.SprintState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findByProjectId(Long projectId);

    List<Sprint> findByProjectIdAndState(Long projectId, SprintState state);
}
