package com.example.prreview.dto.response;

import com.example.prreview.common.ResultCode;

public record ApiResponse<T>(
        int code,
        String message,
        T data
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }
}
