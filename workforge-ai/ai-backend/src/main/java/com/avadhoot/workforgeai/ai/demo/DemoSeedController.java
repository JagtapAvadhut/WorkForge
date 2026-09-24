package com.avadhoot.workforgeai.ai.demo;

import com.avadhoot.workforgeai.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/demo")
public class DemoSeedController {

    private final DemoSeedService demoSeedService;

    public DemoSeedController(DemoSeedService demoSeedService) {
        this.demoSeedService = demoSeedService;
    }

    @PostMapping("/seed")
    public ApiResponse<DemoSeedService.DemoSeedResult> seed(
            @RequestParam(required = false) String sessionId) {
        return ApiResponse.ok(demoSeedService.seed(sessionId));
    }
}
