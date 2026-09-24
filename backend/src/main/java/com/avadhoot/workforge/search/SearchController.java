package com.avadhoot.workforge.search;

import com.avadhoot.workforge.common.dto.ApiResponse;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.project.dto.ProjectDtos.ProjectResponse;
import com.avadhoot.workforge.user.dto.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<GlobalSearchResponse> search(@RequestParam(name = "q", required = false) String q) {
        return ApiResponse.success(searchService.search(q));
    }

    public record GlobalSearchResponse(
            List<IssueResponse> issues,
            List<ProjectResponse> projects,
            List<UserResponse> users) {
    }
}
