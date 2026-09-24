package com.avadhoot.workforge.filter;

import com.avadhoot.workforge.common.dto.PageResponse;
import com.avadhoot.workforge.exception.BusinessException;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.filter.domain.SavedFilter;
import com.avadhoot.workforge.filter.dto.FilterDtos.CreateFilterRequest;
import com.avadhoot.workforge.filter.dto.FilterDtos.FilterResponse;
import com.avadhoot.workforge.filter.repository.SavedFilterRepository;
import com.avadhoot.workforge.issue.IssueService;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FilterService {

    private final SavedFilterRepository filterRepository;
    private final IssueService issueService;

    public FilterService(SavedFilterRepository filterRepository, IssueService issueService) {
        this.filterRepository = filterRepository;
        this.issueService = issueService;
    }

    @Transactional
    public FilterResponse create(CreateFilterRequest request) {
        SavedFilter filter = new SavedFilter();
        filter.setOwnerId(SecurityUtils.requireCurrentUserId());
        filter.setName(request.name());
        filter.setQuery(request.query());
        filter.setShared(request.shared());
        return FilterResponse.from(filterRepository.save(filter));
    }

    @Transactional(readOnly = true)
    public List<FilterResponse> listVisible() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return filterRepository.findVisibleTo(userId).stream().map(FilterResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> run(Long filterId, Pageable pageable) {
        SavedFilter filter = findVisible(filterId);
        return issueService.search(filter.getQuery(), pageable);
    }

    @Transactional
    public void delete(Long filterId) {
        SavedFilter filter = filterRepository.findById(filterId)
                .orElseThrow(() -> new ResourceNotFoundException("Filter", filterId));
        if (!filter.getOwnerId().equals(SecurityUtils.requireCurrentUserId())) {
            throw new BusinessException("You can only delete your own filters");
        }
        filterRepository.delete(filter);
    }

    private SavedFilter findVisible(Long filterId) {
        SavedFilter filter = filterRepository.findById(filterId)
                .orElseThrow(() -> new ResourceNotFoundException("Filter", filterId));
        Long userId = SecurityUtils.requireCurrentUserId();
        if (!filter.isShared() && !filter.getOwnerId().equals(userId)) {
            throw new BusinessException("Filter is not accessible");
        }
        return filter;
    }
}
