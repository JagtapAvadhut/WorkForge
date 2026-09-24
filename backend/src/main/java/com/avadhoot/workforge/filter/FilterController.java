package com.avadhoot.workforge.filter;

import com.avadhoot.workforge.common.dto.ApiResponse;
import com.avadhoot.workforge.common.dto.PageResponse;
import com.avadhoot.workforge.filter.dto.FilterDtos.CreateFilterRequest;
import com.avadhoot.workforge.filter.dto.FilterDtos.FilterResponse;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/v1/filters")
public class FilterController {

    private final FilterService filterService;

    public FilterController(FilterService filterService) {
        this.filterService = filterService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FILTER_MANAGE')")
    public ApiResponse<FilterResponse> create(@Valid @RequestBody CreateFilterRequest request) {
        return ApiResponse.success(filterService.create(request), "Filter saved");
    }

    @GetMapping
    public ApiResponse<List<FilterResponse>> list() {
        return ApiResponse.success(filterService.listVisible());
    }

    @GetMapping("/{id}/run")
    @PreAuthorize("hasAuthority('ISSUE_READ')")
    public ApiResponse<PageResponse<IssueResponse>> run(@PathVariable Long id, Pageable pageable) {
        return ApiResponse.success(filterService.run(id, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FILTER_MANAGE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        filterService.delete(id);
        return ApiResponse.message("Filter deleted");
    }
}
