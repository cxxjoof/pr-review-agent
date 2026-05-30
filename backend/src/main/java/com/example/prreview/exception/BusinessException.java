package com.example.prreview.exception;

import com.example.prreview.common.ResultCode;
import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final ResultCode resultCode;

    public BusinessException(HttpStatus status, ResultCode resultCode, String message) {
        super(message);
        this.status = status;
        this.resultCode = resultCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

    public static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, ResultCode.VALIDATION_ERROR, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, ResultCode.NOT_FOUND, message);
    }
}
