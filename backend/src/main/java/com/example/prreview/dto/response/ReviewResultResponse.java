package com.example.prreview.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReviewResultResponse(
        Long taskId,
        String status,
        String prType,
        String resultViewType,
        Integer findingCount,
        String errorMessage,
        PullRequestPayload pullRequest,
        ReviewReportPayload reviewResult,
        List<FindingPayload> findings,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record PullRequestPayload(
            String repoUrl,
            String repoOwner,
            String repoName,
            Integer prNumber,
            String title,
            String description,
            String author,
            String sourceBranch,
            String targetBranch,
            String state,
            Integer changedFiles,
            Integer additions,
            Integer deletions,
            Integer commits,
            String prUrl
    ) {
    }

    public record ReviewReportPayload(
            String summary,
            List<String> changedModules,
            List<String> reviewSuggestions,
            List<String> testSuggestions,
            String overallConclusion
    ) {
    }

    public record FindingPayload(
            Long id,
            String filePath,
            Integer lineNumber,
            String codeSnippet,
            String findingLevel,
            String findingKind,
            String findingCategory,
            String title,
            String description,
            String suggestion,
            String beforeExample,
            String afterExample,
            String suggestedPatch,
            String diffUrl,
            BigDecimal confidence,
            String feedbackStatus
    ) {
    }
}
