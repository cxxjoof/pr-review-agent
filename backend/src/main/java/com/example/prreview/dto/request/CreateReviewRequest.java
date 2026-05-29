package com.example.prreview.dto.request;

public record CreateReviewRequest(
        String repoUrl,
        Integer prNumber
) {
}
