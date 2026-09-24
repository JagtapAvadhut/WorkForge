package com.avadhoot.workforge.exception;

import com.avadhoot.workforge.common.ErrorCode;

/**
 * Thrown when a requested entity cannot be located.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resource, Object identifier) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "%s not found: %s".formatted(resource, identifier));
    }
}
