package com.avadhoot.workforgeai.ai.mcp;

public class McpUnavailableException extends RuntimeException {
    public McpUnavailableException(String message) {
        super(message);
    }

    public McpUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
