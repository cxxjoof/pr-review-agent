package com.example.prreview.dto.response;

import java.time.LocalDateTime;

public record ReviewTaskResponse(
        Long taskId,
        String repoUrl,
        String repoOwner,
        String repoName,
        Integer prNumber,
        String status,
        Integer riskCount,
        String summary,
        String overallConclusion,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
