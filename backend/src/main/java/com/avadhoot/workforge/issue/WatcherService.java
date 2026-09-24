package com.avadhoot.workforge.issue;

import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.issue.domain.Watcher;
import com.avadhoot.workforge.issue.repository.IssueRepository;
import com.avadhoot.workforge.issue.repository.WatcherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WatcherService {

    private final WatcherRepository watcherRepository;
    private final IssueRepository issueRepository;

    public WatcherService(WatcherRepository watcherRepository, IssueRepository issueRepository) {
        this.watcherRepository = watcherRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional
    public void watch(Long issueId, Long userId) {
        if (issueRepository.findById(issueId).isEmpty()) {
            throw new ResourceNotFoundException("Issue", issueId);
        }
        if (!watcherRepository.existsByIssueIdAndUserId(issueId, userId)) {
            Watcher watcher = new Watcher();
            watcher.setIssueId(issueId);
            watcher.setUserId(userId);
            watcherRepository.save(watcher);
        }
    }

    @Transactional
    public void unwatch(Long issueId, Long userId) {
        watcherRepository.findByIssueIdAndUserId(issueId, userId)
                .ifPresent(watcherRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<Long> watchers(Long issueId) {
        return watcherRepository.findByIssueId(issueId).stream()
                .map(Watcher::getUserId)
                .toList();
    }
}
