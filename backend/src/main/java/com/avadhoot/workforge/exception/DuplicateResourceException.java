package com.avadhoot.workforge.exception;

import com.avadhoot.workforge.common.ErrorCode;

/**
 * Thrown when attempting to create a resource that already exists.
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
