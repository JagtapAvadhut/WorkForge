package com.avadhoot.workforge.exception;

import com.avadhoot.workforge.common.ErrorCode;

/**
 * Thrown when a workflow transition between two statuses is not permitted.
 */
public class InvalidTransitionException extends BusinessException {

    public InvalidTransitionException(String message) {
        super(ErrorCode.INVALID_TRANSITION, message);
    }
}
