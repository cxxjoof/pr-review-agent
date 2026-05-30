package com.example.prreview.common;

public enum ResultCode {
    SUCCESS(200, "success"),
    VALIDATION_ERROR(400, "validation failed"),
    UNAUTHORIZED(401, "unauthorized"),
    NOT_FOUND(404, "resource not found"),
    RATE_LIMITED(429, "too many requests"),
    GITHUB_API_ERROR(502, "github api request failed"),
    MODEL_API_ERROR(502, "model api request failed"),
    GATEWAY_TIMEOUT(504, "upstream request timed out"),
    INTERNAL_ERROR(500, "internal server error");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
