package com.example.prreview.dto.response;

import java.time.LocalDateTime;

public record ReviewTaskResponse(
        Long taskId,
        String repoUrl,
        String repoOwner,
        String repoName,
        Integer prNumber,
        String prType,
        String resultViewType,
        String status,
        Integer findingCount,
        String summary,
        String overallConclusion,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
