package com.avadhoot.workforge.project;

import com.avadhoot.workforge.project.domain.ProjectIssueSeq;
import com.avadhoot.workforge.project.repository.ProjectIssueSeqRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Allocates gap-free, collision-free per-project issue numbers by pessimistically
 * locking the project's counter row within the caller's transaction.
 */
@Service
public class IssueSequenceService {

    private final ProjectIssueSeqRepository seqRepository;

    public IssueSequenceService(ProjectIssueSeqRepository seqRepository) {
        this.seqRepository = seqRepository;
    }

    /** Initialises the counter row for a new project (idempotent). */
    @Transactional
    public void initialise(Long projectId) {
        if (!seqRepository.existsById(projectId)) {
            ProjectIssueSeq seq = new ProjectIssueSeq();
            seq.setProjectId(projectId);
            seq.setSeq(0L);
            seqRepository.save(seq);
        }
    }

    /**
     * Returns the next sequence value for the project. Uses SELECT ... FOR UPDATE so
     * concurrent callers are serialised and never receive duplicate values.
     */
    @Transactional
    public long next(Long projectId) {
        ProjectIssueSeq seq = seqRepository.findByProjectIdForUpdate(projectId)
                .orElseGet(() -> {
                    ProjectIssueSeq created = new ProjectIssueSeq();
                    created.setProjectId(projectId);
                    created.setSeq(0L);
                    return seqRepository.saveAndFlush(created);
                });
        long next = seq.getSeq() + 1;
        seq.setSeq(next);
        seqRepository.saveAndFlush(seq);
        return next;
    }
}
