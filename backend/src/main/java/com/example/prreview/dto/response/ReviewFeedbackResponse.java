package com.example.prreview.dto.response;

import java.time.LocalDateTime;

public record ReviewFeedbackResponse(
        Long taskId,
        Long findingId,
        String feedbackType,
        String comment,
        LocalDateTime createdAt
) {
}
