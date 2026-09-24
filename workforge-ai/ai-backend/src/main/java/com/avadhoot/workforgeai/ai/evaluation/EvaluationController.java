package com.avadhoot.workforgeai.ai.evaluation;

import com.avadhoot.workforgeai.ai.evaluation.dto.EvaluationRunRequest;
import com.avadhoot.workforgeai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * Starts an evaluation job asynchronously and returns runId immediately.
     * Poll {@code GET /runs/{runId}} for progress and results.
     */
    @PostMapping("/run")
    public ApiResponse<EvaluationRunSnapshot> run(@Valid @RequestBody(required = false) EvaluationRunRequest request) {
        String suite = request == null || request.suite() == null ? "ALL" : request.suite();
        return ApiResponse.ok(evaluationService.startAsync(suite));
    }

    @GetMapping("/runs/{runId}")
    public ApiResponse<EvaluationRunSnapshot> getRun(@PathVariable String runId) {
        return ApiResponse.ok(evaluationService.getRun(runId));
    }
}
