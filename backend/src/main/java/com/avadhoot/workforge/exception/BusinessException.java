package com.avadhoot.workforge.exception;

import com.avadhoot.workforge.common.ErrorCode;

/**
 * Base runtime exception for domain / business-rule violations.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message) {
        this(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
