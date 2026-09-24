package com.avadhoot.workforgeai.ai.security;

import com.avadhoot.workforgeai.ai.security.dto.SecurityCheckRequest;
import com.avadhoot.workforgeai.ai.security.dto.SecurityCheckResponse;
import com.avadhoot.workforgeai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/security")
public class SecurityController {

    private final SecurityPolicyService securityPolicyService;

    public SecurityController(SecurityPolicyService securityPolicyService) {
        this.securityPolicyService = securityPolicyService;
    }

    @PostMapping("/check")
    public ApiResponse<SecurityCheckResponse> check(@Valid @RequestBody SecurityCheckRequest request) {
        SecurityCheckResult result = securityPolicyService.checkInput(request.input());
        return ApiResponse.ok(new SecurityCheckResponse(
                result.status().name(),
                result.reason(),
                result.allowed()));
    }

    @GetMapping("/policies")
    public ApiResponse<Map<String, Object>> policies() {
        return ApiResponse.ok(securityPolicyService.policies());
    }
}
