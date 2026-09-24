package com.avadhoot.workforgeai.common;

import com.avadhoot.workforgeai.ai.mcp.McpToolException;
import com.avadhoot.workforgeai.ai.mcp.McpUnavailableException;
import com.avadhoot.workforgeai.ai.security.SecurityBlockedException;
import com.avadhoot.workforgeai.ai.security.SecurityPolicyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final SecurityPolicyService securityPolicyService;

    public GlobalExceptionHandler(SecurityPolicyService securityPolicyService) {
        this.securityPolicyService = securityPolicyService;
    }

    /** Test helper when no Spring context is available. */
    public GlobalExceptionHandler() {
        this(new SecurityPolicyService(
                "getIssue,searchIssues,getProject",
                4000,
                10,
                5,
                10,
                true));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(message.isBlank() ? "Validation failed" : message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        HttpStatus status = ex.getMessage() != null && ex.getMessage().startsWith("Document not found")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
        if (ex.getMessage() != null && ex.getMessage().startsWith("Trace not found")) {
            status = HttpStatus.NOT_FOUND;
        }
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(securityPolicyService.sanitizeErrorMessage(ex.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String message = cause.getMessage() != null ? cause.getMessage() : "Invalid request body";
        if (message.contains("PromptStrategy") || message.toLowerCase().contains("strategy")) {
            message = "Invalid strategy. Allowed: GENERAL, DOMAIN_EXPERT, CONCISE, DETAILED, FEW_SHOT, STRUCTURED";
        } else {
            message = "Invalid request body";
        }
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(SecurityBlockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurity(SecurityBlockedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(securityPolicyService.sanitizeErrorMessage(ex.reason())));
    }

    @ExceptionHandler(McpUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMcpUnavailable(McpUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail(securityPolicyService.sanitizeErrorMessage(ex.getMessage())));
    }

    @ExceptionHandler(McpToolException.class)
    public ResponseEntity<ApiResponse<Void>> handleMcpTool(McpToolException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(securityPolicyService.sanitizeErrorMessage(ex.getMessage())));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail(securityPolicyService.sanitizeErrorMessage(ex.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("AI request failed"));
    }
}
