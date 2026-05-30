package com.example.prreview.exception;

import com.example.prreview.common.ResultCode;
import org.springframework.http.HttpStatus;

public class GitHubApiException extends BusinessException {

    public GitHubApiException(HttpStatus status, ResultCode resultCode, String message) {
        super(status, resultCode, message);
    }
}
