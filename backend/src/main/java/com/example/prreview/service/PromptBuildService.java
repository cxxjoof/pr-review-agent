package com.example.prreview.service;

import com.example.prreview.dto.diff.ReviewContext;
import com.example.prreview.dto.model.ChatCompletionRequest;
import com.example.prreview.dto.model.ChatMessage;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PromptBuildService {

    private static final double REVIEW_TEMPERATURE = 0.1D;
    private static final int REVIEW_MAX_TOKENS = 2400;

    public ChatCompletionRequest buildReviewRequest(ReviewContext reviewContext) {
        if (reviewContext == null) {
            throw new IllegalArgumentException("Review context must not be null.");
        }

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setTemperature(REVIEW_TEMPERATURE);
        request.setMaxTokens(REVIEW_MAX_TOKENS);
        request.setResponseFormat(new ChatCompletionRequest.ResponseFormat("json_object"));
        request.setMessages(List.of(
                new ChatMessage("system", buildSystemPrompt()),
                new ChatMessage("user", buildUserPrompt(reviewContext))
        ));
        return request;
    }

    private String buildSystemPrompt() {
        return """
                You are a senior pull request reviewer.
                Review only the provided pull request context and diff excerpt.
                Focus on correctness, exception handling, input validation, security, performance, database impact, configuration changes, and missing tests.
                Do not invent issues when the evidence is weak.
                When a concern is uncertain, phrase it conservatively in the description or suggestion.
                Return valid JSON only, without markdown code fences or explanatory prose.

                Use this exact JSON shape:
                {
                  "summary": "string",
                  "changedModules": ["string"],
                  "riskItems": [
                    {
                      "filePath": "string",
                      "lineNumber": 123,
                      "codeSnippet": "string or null",
                      "riskLevel": "HIGH|MEDIUM|LOW",
                      "riskType": "NULL_POINTER|EXCEPTION_HANDLING|SECURITY|PERFORMANCE|INPUT_VALIDATION|DATABASE|CONFIG_CHANGE|TEST_MISSING|CODE_STYLE|OTHER",
                      "description": "string",
                      "suggestion": "string",
                      "confidence": 0.85
                    }
                  ],
                  "reviewSuggestions": ["string"],
                  "testSuggestions": ["string"],
                  "overallConclusion": "string"
                }

                Constraints:
                - Keep summary concise and specific to this PR.
                - riskItems can be an empty array.
                - reviewSuggestions and testSuggestions should be actionable.
                - Only mention files that appear in the provided PR context.
                """;
    }

    private String buildUserPrompt(ReviewContext reviewContext) {
        return """
                Analyze the following GitHub pull request context and generate a structured review report.

                Metadata:
                - Repository: %s/%s
                - PR Number: %s
                - Title: %s
                - Author: %s
                - Branches: %s -> %s
                - Changed files: %s
                - Additions: %s
                - Deletions: %s
                - Commits: %s
                - Parsed files: %s
                - Files without patch: %s
                - Change scale: %s

                Structured PR context:
                %s
                """.formatted(
                defaultText(reviewContext.repoOwner()),
                defaultText(reviewContext.repoName()),
                defaultNumber(reviewContext.prNumber()),
                defaultText(reviewContext.title()),
                defaultText(reviewContext.author()),
                defaultText(reviewContext.sourceBranch()),
                defaultText(reviewContext.targetBranch()),
                defaultNumber(reviewContext.changedFiles()),
                defaultNumber(reviewContext.additions()),
                defaultNumber(reviewContext.deletions()),
                defaultNumber(reviewContext.commits()),
                defaultNumber(reviewContext.parsedFileCount()),
                defaultNumber(reviewContext.patchlessFileCount()),
                defaultText(reviewContext.changeScale()),
                defaultText(reviewContext.aiContext())
        );
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private int defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }
}
