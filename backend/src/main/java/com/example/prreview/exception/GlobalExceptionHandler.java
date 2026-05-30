package com.example.prreview.exception;

import com.example.prreview.common.ResultCode;
import com.example.prreview.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return buildResponse(exception.getStatus(), exception.getResultCode(), exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return buildResponse(status, mapResultCode(status), exception.getReason());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .distinct()
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, ResultCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, ResultCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        String message = exception.getMessage() != null
                && exception.getMessage().contains("Required request body is missing")
                ? "Request body must not be empty."
                : "Request body is malformed or contains invalid field values.";
        return buildResponse(HttpStatus.BAD_REQUEST, ResultCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ResultCode.VALIDATION_ERROR,
                "%s is required.".formatted(exception.getParameterName())
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ResultCode.VALIDATION_ERROR,
                "%s has an invalid value.".formatted(exception.getName())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResultCode.INTERNAL_ERROR,
                "Internal server error. Please retry later."
        );
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(HttpStatus status, ResultCode resultCode, String message) {
        String responseMessage = StringUtils.hasText(message) ? message : resultCode.getMessage();
        return ResponseEntity.status(status).body(ApiResponse.error(resultCode, responseMessage));
    }

    private String formatFieldError(FieldError fieldError) {
        if (fieldError == null) {
            return ResultCode.VALIDATION_ERROR.getMessage();
        }

        String defaultMessage = fieldError.getDefaultMessage();
        if (!StringUtils.hasText(defaultMessage)) {
            return "%s is invalid.".formatted(fieldError.getField());
        }

        return defaultMessage;
    }

    private ResultCode mapResultCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> ResultCode.VALIDATION_ERROR;
            case UNAUTHORIZED -> ResultCode.UNAUTHORIZED;
            case NOT_FOUND -> ResultCode.NOT_FOUND;
            case TOO_MANY_REQUESTS -> ResultCode.RATE_LIMITED;
            case GATEWAY_TIMEOUT -> ResultCode.GATEWAY_TIMEOUT;
            case BAD_GATEWAY -> ResultCode.GITHUB_API_ERROR;
            default -> ResultCode.INTERNAL_ERROR;
        };
    }
}
