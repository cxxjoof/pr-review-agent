package com.example.prreview.dto.request;

import com.example.prreview.enums.FeedbackType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewFeedbackRequest(
        @NotNull(message = "feedbackType must not be null.")
        FeedbackType feedbackType,
        @Size(max = 1000, message = "comment must be at most 1000 characters.")
        String comment
) {
}
